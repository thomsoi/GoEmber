package com.goember.hackathon.ember;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ember")
public class EmberController {

    private final EmberService emberService;

    public EmberController(EmberService emberService) {
        this.emberService = emberService;
    }

    @GetMapping("/stops")
    public List<Map> getStops() {
        return emberService.getStopPoints();
    }
}