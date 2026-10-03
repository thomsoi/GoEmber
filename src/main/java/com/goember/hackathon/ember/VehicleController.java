package com.goember.hackathon.ember;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/live")
    public List<LiveBus> getLiveBuses() {
        return emberService.getLiveBuses();
    }
}