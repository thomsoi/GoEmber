package com.goember.hackathon.ember;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ember")
public class EmberController {

    private final EmberService emberService;

    public EmberController(EmberService emberService) {
        this.emberService = emberService;
    }

    @GetMapping("/stops")
    public List<Map<String, Object>> getStops() {
        return emberService.getStopPoints();
    }

    @GetMapping("/stops/nearby")
    public List<Map<String, Object>> getNearbyStops(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "0.5") double radiusKm) {
        return emberService.getStopPoints().stream()
                .filter(location -> location != null)
                .filter(location -> isWithinRadius(
                        latitude,
                        longitude,
                        asDouble(location.get("lat")),
                        asDouble(location.get("lon")),
                        radiusKm))
                .toList();
    }

    @GetMapping("/trip/{tripId}")
    public Map<String, Object> getTrip(@PathVariable Long tripId) {
        return emberService.getTrip(tripId);
    }

    private boolean isWithinRadius(
            double lat1,
            double lon1,
            double lat2,
            double lon2,
            double radiusKm) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (earthRadius * c) <= radiusKm;
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }
}