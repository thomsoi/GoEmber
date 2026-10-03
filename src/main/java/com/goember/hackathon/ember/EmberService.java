package com.goember.hackathon.ember;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.goember.hackathon.ember.proto.GPSInfo;
import com.goember.hackathon.ember.proto.LiveVehicleData;
import com.goember.hackathon.ember.proto.MinimalLocationTime;
import com.goember.hackathon.ember.proto.MinimalVehicleTrip;
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
        if (origin == null || origin.isBlank() || destination == null || destination.isBlank()) {
            return List.of();
        }

        String normalizedOrigin = normalizeLocationQuery(origin);
        String normalizedDestination = normalizeLocationQuery(destination);
        return emberClient.getLiveVehicles().getVehiclesList().stream()
                .filter(vehicle -> vehicle.hasTrips() && vehicle.getTrips().hasActive())
                .map(this::toLiveBus)
                .filter(bus -> servesRoute(bus.route(), normalizedOrigin, normalizedDestination))
                .toList();
    }

    private boolean servesRoute(List<BusStop> route, String origin, String destination) {
        boolean originFound = false;
        for (BusStop stop : route) {
            if (!originFound) {
                originFound = matchesLocation(stop, origin);
            } else if (matchesLocation(stop, destination)) {
                return true;
            }
        }
        return false;
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

    private LiveBus toLiveBus(LiveVehicleData vehicle) {
        MinimalVehicleTrip trip = vehicle.getTrips().getActive();
        GPSInfo gps = vehicle.hasGps() ? vehicle.getGps() : null;
        return new LiveBus(
                Integer.toUnsignedLong(vehicle.getId()),
                vehicle.getPlateNumber(),
                trip.getUid(),
                trip.getRouteNumber(),
                trip.hasCurrentStop() ? toBusStop(trip.getCurrentStop()) : null,
                trip.hasNextStop() ? toBusStop(trip.getNextStop()) : null,
                trip.getRouteList().stream().map(this::toBusStop).toList(),
                gps == null ? null : gps.getLatitude(),
                gps == null ? null : gps.getLongitude(),
                gps == null || !gps.hasLastUpdated() ? null : toInstant(gps.getLastUpdated().getSeconds(), gps.getLastUpdated().getNanos())
        );
    }

    private BusStop toBusStop(MinimalLocationTime stop) {
        return new BusStop(
                Integer.toUnsignedLong(stop.getId()),
                stop.hasLocation() ? asString(stop.getLocation().getName()) : "",
                stop.hasLocation() ? asString(stop.getLocation().getRegionName()) : ""
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
            Instant positionUpdatedAt) {
    }

    public record BusStop(long locationId, String name, String regionName) {
    }
}