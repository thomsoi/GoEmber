package com.goember.hackathon.stop;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopService stopService;

    public StopController(StopService stopService) {
        this.stopService = stopService;
    }

    @GetMapping
    public List<Stop> getStops() {
        return stopService.getStops();
    }

    @GetMapping("/nearest")
    public Stop getNearestStop(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return stopService.findNearestStop(latitude, longitude);
    }

    @GetMapping("/nearby")
    public List<Stop> getNearbyStops(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "0.5") double radiusKm
    ) {
        return stopService.findStopsWithinRadius(
                latitude,
                longitude,
                radiusKm
        );
    }
}