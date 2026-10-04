package com.goember.hackathon.ember;

import java.time.Instant;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.goember.hackathon.ember.proto.GPSInfo;
import com.goember.hackathon.ember.proto.LiveVehicleData;
import com.goember.hackathon.ember.proto.MinimalVehicleTrip;
import com.goember.hackathon.ember.proto.MinimalLocationTime;
import com.goember.hackathon.ember.proto.ProtoTimestamp;
import com.goember.hackathon.stop.Stop;

@Service
public class EmberService {

    private final EmberClient emberClient;

    public EmberService(EmberClient emberClient) {
        this.emberClient = emberClient;
    }

    public List<Map<String, Object>> getStopPoints() {
        return emberClient.searchLocations(
                "",
                50,
                "STOP_POINT"
        );
    }

    public List<Stop> getStops() {
        List<Map<String, Object>> locations = getStopPoints();

        if (locations == null) {
            return List.of();
        }

        return locations.stream()
                .filter(location -> location != null)
                .map(this::toStop)
                .toList();
    }

    public Map<String, Object> getTrip(Long tripId) {
        return emberClient.getTrip(tripId);
    }

    public List<LiveBus> getLiveBuses(String origin, String destination) {
        return getLiveBuses(origin, destination, "");
    }

    public List<LiveBus> getLiveBuses(String origin, String destination, String trackedTripUid) {
        return getLiveBuses(origin, destination, trackedTripUid, trackedTripUid, Instant.now());
    }

    public List<LiveBus> getLiveBuses(String origin, String destination, String trackedTripUid, String detailsTripUid) {
        return getLiveBuses(origin, destination, trackedTripUid, detailsTripUid, Instant.now());
    }

    List<LiveBus> getLiveBuses(String origin, String destination, Instant now) {
        return getLiveBuses(origin, destination, "", now);
    }

    List<LiveBus> getLiveBuses(String origin, String destination, String trackedTripUid, Instant now) {
        return getLiveBuses(origin, destination, trackedTripUid, trackedTripUid, now);
    }

    List<LiveBus> getLiveBuses(String origin, String destination, String trackedTripUid, String detailsTripUid, Instant now) {
        String from = origin == null ? "" : normalizeLocationQuery(origin);
        String to = destination == null ? "" : normalizeLocationQuery(destination);
        var candidates = new ArrayList<VehicleTrip>();
        for (var vehicle : emberClient.getLiveVehicles().getVehiclesList()) {
            if (!vehicle.hasTrips()) continue;
            if (vehicle.getTrips().hasActive()) candidates.add(new VehicleTrip(vehicle, vehicle.getTrips().getActive(), false));
            if (vehicle.getTrips().hasNext()) candidates.add(new VehicleTrip(vehicle, vehicle.getTrips().getNext(), true));
        }
        if (candidates.isEmpty()) return List.of();
        var requested = candidates.stream().map(item -> item.trip().getUid())
                .filter(uid -> uid.equals(detailsTripUid) || uid.equals(trackedTripUid)).distinct().toList();
        var details = requested.isEmpty() ? Map.<String, Map<String, Object>>of() : emberClient.getLiveTripDetails(requested);
        var buses = new ArrayList<LiveBus>();
        for (var candidate : candidates) {
            var trip = candidate.trip();
            boolean detailsLoaded = details.containsKey(trip.getUid());
            var route = detailsLoaded ? readRoute(details.get(trip.getUid())) : readLiveRoute(trip);
            var current = trip.hasCurrentStop() ? findEvent(route, Integer.toUnsignedLong(trip.getCurrentStop().getId())) : null;
            var next = trip.hasNextStop() ? findEvent(route, Integer.toUnsignedLong(trip.getNextStop().getId())) : null;
            RouteStop departure = null;
            Instant departureTime = null;
            boolean serves = false;
            for (int i = 0; i < route.size() - 1; i++) {
                var stop = route.get(i);
                if (!stop.boarding() || stop.skipped() || (!from.isEmpty() && !matchesLocation(stop.stop(), from))) continue;
                boolean reachesDestination = to.isEmpty();
                for (int j = i + 1; j < route.size() && !reachesDestination; j++) {
                    var end = route.get(j);
                    reachesDestination = end.dropOff() && !end.skipped() && matchesLocation(end.stop(), to);
                }
                if (!reachesDestination) continue;
                serves = true;
                if (stop.actual() != null || stop.time() == null) continue;
                Instant time = stop.time();
                if (time.isBefore(now)) {
                    if (current == null || current.eventId() != stop.eventId()) continue;
                    time = now; // At the stop, overdue, and no recorded departure yet.
                }
                if (departureTime == null || time.isBefore(departureTime)) {
                    departure = stop;
                    departureTime = time;
                }
            }
            // Preserve visibility of ongoing rides for fully specified route searches.
            boolean tracked = !candidate.upcoming() && trip.getUid().equals(trackedTripUid);
            if (departure == null && !tracked && (candidate.upcoming() || from.isEmpty() || to.isEmpty() || !serves)) continue;
            buses.add(toLiveBus(candidate, route, current, next, departure, departureTime, detailsLoaded));
        }
        return buses.stream().sorted(Comparator.comparing(LiveBus::departureTime,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparingLong(LiveBus::vehicleId)
                .thenComparing(LiveBus::tripUid)).toList();
    }

    private RouteStop findEvent(List<RouteStop> route, long eventId) {
        return route.stream().filter(stop -> stop.eventId() == eventId).findFirst().orElse(null);
    }

    private List<RouteStop> readLiveRoute(MinimalVehicleTrip trip) {
        var stops = new ArrayList<>(trip.getRouteList());
        if (trip.hasCurrentStop()) mergeLiveStop(stops, trip.getCurrentStop());
        if (trip.hasNextStop()) mergeLiveStop(stops, trip.getNextStop());
        return stops.stream().map(stop -> {
            var times = stop.getDeparture();
            Instant time = times.hasEstimated() ? timestamp(times.getEstimated())
                    : times.hasScheduled() ? timestamp(times.getScheduled()) : null;
            return new RouteStop(Integer.toUnsignedLong(stop.getId()), new BusStop(null,
                    stop.getLocation().getName(), stop.getLocation().getRegionName(), Integer.toUnsignedLong(stop.getId())),
                    stop.getAllowBoarding(), stop.getAllowDropOff(), false, time,
                    times.hasActual() ? timestamp(times.getActual()) : null);
        }).toList();
    }

    private void mergeLiveStop(List<MinimalLocationTime> stops, MinimalLocationTime observed) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getId() == observed.getId()) {
                stops.set(i, observed);
                return;
            }
        }
        // The public feed gives endpoints, plus the current and next stop in between.
        stops.add(Math.max(0, stops.size() - 1), observed);
    }

    private Instant timestamp(ProtoTimestamp value) {
        try { return Instant.ofEpochSecond(value.getSeconds(), value.getNanos()); }
        catch (DateTimeException exception) { throw invalidTrip(); }
    }

    private List<RouteStop> readRoute(Map<String, Object> detail) {
        if (detail == null || !(detail.get("route") instanceof List<?> rows)) throw invalidTrip();
        var route = new ArrayList<RouteStop>();
        for (var row : rows) {
            if (!(row instanceof Map<?, ?> stop) || !(stop.get("id") instanceof Number eventId)
                    || !(stop.get("location") instanceof Map<?, ?> location)
                    || !(location.get("id") instanceof Number locationId)
                    || !(location.get("name") instanceof String name) || name.isBlank()
                    || !(stop.get("departure") instanceof Map<?, ?> times)
                    || !(stop.get("allow_boarding") instanceof Boolean boarding)
                    || !(stop.get("allow_drop_off") instanceof Boolean dropOff)) throw invalidTrip();
            Instant estimated = readTime(times.get("estimated"));
            Instant scheduled = readTime(times.get("scheduled"));
            route.add(new RouteStop(eventId.longValue(), new BusStop(locationId.longValue(), name,
                    asString(location.get("region_name")), eventId.longValue()), boarding, dropOff, Boolean.TRUE.equals(stop.get("skipped")),
                    estimated == null ? scheduled : estimated, readTime(times.get("actual"))));
        }
        return route;
    }

    private Instant readTime(Object value) {
        if (value == null) return null;
        if (!(value instanceof String text)) throw invalidTrip();
        try { return Instant.parse(text); }
        catch (DateTimeException exception) { throw invalidTrip(); }
    }

    private EmberApiException invalidTrip() {
        return new EmberApiException("Ember returned invalid trip details. Please try again later.", null, false);
    }

    private boolean matchesLocation(BusStop stop, String query) {
        return containsQuery(stop.name(), query) || containsQuery(stop.regionName(), query);
    }

    private boolean containsQuery(String value, String query) {
        return value != null && normalizeLocationQuery(value).contains(query);
    }

    private String normalizeLocationQuery(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private LiveBus toLiveBus(VehicleTrip candidate, List<RouteStop> route, RouteStop current,
            RouteStop next, RouteStop departure, Instant departureTime, boolean detailsLoaded) {
        LiveVehicleData vehicle = candidate.vehicle();
        MinimalVehicleTrip trip = candidate.trip();
        GPSInfo gps = vehicle.hasGps() ? vehicle.getGps() : null;
        return new LiveBus(
                Integer.toUnsignedLong(vehicle.getId()),
                vehicle.getPlateNumber(),
                trip.getUid(),
                trip.getRouteNumber(),
                current == null ? null : current.stop(),
                next == null ? null : next.stop(),
                route.stream().filter(stop -> !stop.skipped()).map(RouteStop::stop).toList(),
                gps == null ? null : gps.getLatitude(),
                gps == null ? null : gps.getLongitude(),
                gps == null || !gps.hasLastUpdated() ? null : toInstant(gps.getLastUpdated().getSeconds(), gps.getLastUpdated().getNanos()),
                departure == null ? null : departure.stop(), departureTime, candidate.upcoming(), detailsLoaded
        );
    }

    private Instant toInstant(long seconds, int nanos) {
        return Instant.ofEpochSecond(seconds, nanos);
    }

    private Stop toStop(Map<String, Object> location) {
        return new Stop(
                asLong(location.get("id")),
                asString(location.get("name")),
                asString(location.get("type")),
                asLong(location.get("area_id")),
                asDouble(location.get("lat")),
                asDouble(location.get("lon"))
        );
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    public record LiveBus(
            long vehicleId,
            String plateNumber,
            String tripUid,
            String routeNumber,
            BusStop currentStop,
            BusStop nextStop,
            List<BusStop> route,
            Double latitude,
            Double longitude,
            Instant positionUpdatedAt,
            BusStop departureStop,
            Instant departureTime,
            boolean upcomingTrip,
            boolean routeDetailsLoaded) {
    }

    public record BusStop(Long locationId, String name, String regionName, long eventId) {
    }

    private record VehicleTrip(LiveVehicleData vehicle, MinimalVehicleTrip trip, boolean upcoming) {}
    private record RouteStop(long eventId, BusStop stop, boolean boarding, boolean dropOff,
            boolean skipped, Instant time, Instant actual) {}
}
