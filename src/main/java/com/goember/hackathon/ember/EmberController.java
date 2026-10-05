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
        return emberService.getNearbyStopPoints(latitude, longitude, radiusKm);
    }

    @GetMapping("/trip/{tripId}")
    public Map<String, Object> getTrip(@PathVariable Long tripId) {
        return emberService.getTrip(tripId);
    }

}
