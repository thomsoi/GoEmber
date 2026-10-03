package com.goember.hackathon.journey;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @PostMapping
    public Journey createJourney(@Valid @RequestBody CreateJourneyRequest request) {
        return journeyService.createJourney(
                request.userId(),
                request.emberTripId(),
                request.originLocationId(),
                request.destinationLocationId(),
                request.originName(),
                request.destinationName(),
                request.scheduledDeparture(),
                request.scheduledArrival()
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

    public record CreateJourneyRequest(
            @NotNull Long userId,
            @NotNull Long emberTripId,
            @NotNull Long originLocationId,
            @NotNull Long destinationLocationId,
            @NotBlank String originName,
            @NotBlank String destinationName,
            @NotNull LocalDateTime scheduledDeparture,
            @NotNull LocalDateTime scheduledArrival
    ) {}
}
