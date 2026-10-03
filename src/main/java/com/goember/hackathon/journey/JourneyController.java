package com.goember.hackathon.journey;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @PostMapping
    public Journey createJourney(
            @RequestParam Long userId,
            @RequestParam Long emberTripId,
            @RequestParam Long originLocationId,
            @RequestParam Long destinationLocationId,
            @RequestParam String originName,
            @RequestParam String destinationName,
            @RequestParam LocalDateTime scheduledDeparture,
            @RequestParam LocalDateTime scheduledArrival
    ) {
        return journeyService.createJourney(
                userId,
                emberTripId,
                originLocationId,
                destinationLocationId,
                originName,
                destinationName,
                scheduledDeparture,
                scheduledArrival
        );
    }

    @GetMapping("/{journeyId}")
    public Journey getJourney(@PathVariable Long journeyId) {
        return journeyService.getJourney(journeyId);
    }

    @GetMapping("/active")
    public Optional<Journey> getActiveJourney(@RequestParam Long userId) {
        return journeyService.getActiveJourney(userId);
    }

    @PostMapping("/{journeyId}/start")
    public Journey startJourney(@PathVariable Long journeyId) {
        return journeyService.startJourney(journeyId);
    }

    @PostMapping("/{journeyId}/complete")
    public Journey completeJourney(@PathVariable Long journeyId) {
        return journeyService.completeJourney(journeyId);
    }
}
