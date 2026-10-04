package com.goember.hackathon.ember;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goember.hackathon.ember.EmberService.LiveBus;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final EmberService emberService;

    public VehicleController(EmberService emberService) {
        this.emberService = emberService;
    }

    @GetMapping(value = "/live", produces = "application/json")
    public List<LiveBus> getLiveBuses(
            @RequestParam(name = "origin", required = false, defaultValue = "") String origin,
            @RequestParam(name = "destination", required = false, defaultValue = "") String destination,
            @RequestParam(name = "trackedTripUid", required = false, defaultValue = "") String trackedTripUid,
            @RequestParam(name = "detailsTripUid", required = false, defaultValue = "") String detailsTripUid) {
        return emberService.getLiveBuses(origin, destination, trackedTripUid, detailsTripUid);
    }
}
