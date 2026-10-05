package com.goember.hackathon.ember;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.goember.hackathon.ember.EmberService.BusStop;
import com.goember.hackathon.ember.proto.MinimalLocationTime;
import com.goember.hackathon.ember.proto.MinimalVehicleTrip;
import com.goember.hackathon.ember.proto.ProtoTimestamp;

/** Converts the sparse protobuf feed and full JSON details into ordered route events. */
final class EmberRouteMapper {
    private EmberRouteMapper() {
    }

    static List<RouteStop> readLiveRoute(MinimalVehicleTrip trip) {
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

    private static void mergeLiveStop(List<MinimalLocationTime> stops, MinimalLocationTime observed) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getId() == observed.getId()) {
                stops.set(i, observed);
                return;
            }
        }
        // The public feed gives endpoints, plus the current and next stop in between.
        stops.add(Math.max(0, stops.size() - 1), observed);
    }

    private static Instant timestamp(ProtoTimestamp value) {
        try { return Instant.ofEpochSecond(value.getSeconds(), value.getNanos()); }
        catch (DateTimeException exception) { throw invalidTrip(); }
    }

    static List<RouteStop> readRoute(Map<String, Object> detail) {
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
                    location.get("region_name") == null ? null : location.get("region_name").toString(), eventId.longValue()), boarding, dropOff, Boolean.TRUE.equals(stop.get("skipped")),
                    estimated == null ? scheduled : estimated, readTime(times.get("actual"))));
        }
        return route;
    }

    private static Instant readTime(Object value) {
        if (value == null) return null;
        if (!(value instanceof String text)) throw invalidTrip();
        try { return Instant.parse(text); }
        catch (DateTimeException exception) { throw invalidTrip(); }
    }

    private static EmberApiException invalidTrip() {
        return new EmberApiException("Ember returned invalid trip details. Please try again later.", null, false);
    }

    record RouteStop(long eventId, BusStop stop, boolean boarding, boolean dropOff,
            boolean skipped, Instant time, Instant actual) {}
}
