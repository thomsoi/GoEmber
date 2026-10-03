package com.goember.hackathon.ember;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

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
}