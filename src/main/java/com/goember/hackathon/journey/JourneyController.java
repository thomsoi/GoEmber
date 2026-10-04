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
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.RequestHeader;
import com.goember.hackathon.user.GuestAccess;

@RestController
@RequestMapping("/api/journeys")
public class JourneyController {

    private final JourneyService journeyService;
    private final GuestAccess access;

    public JourneyController(JourneyService journeyService, GuestAccess access) {
        this.journeyService = journeyService;
        this.access = access;
    }

    @PostMapping
    public Journey createJourney(@Valid @RequestBody CreateJourneyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        access.requireUser(request.userId(), authorization);
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
    public Journey getJourney(@PathVariable Long journeyId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Journey journey = journeyService.getJourney(journeyId);
        access.requireUser(journey.getUserId(), authorization);
        return journey;
    }

    @GetMapping("/active")
    public Optional<Journey> getActiveJourney(@RequestParam Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        access.requireUser(userId, authorization);
        return journeyService.getActiveJourney(userId);
    }

    @PostMapping("/{journeyId}/start")
    public Journey startJourney(@PathVariable Long journeyId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        access.requireUser(journeyService.getJourney(journeyId).getUserId(), authorization);
        return journeyService.startJourney(journeyId);
    }

    @PostMapping("/{journeyId}/complete")
    public Journey completeJourney(@PathVariable Long journeyId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        access.requireUser(journeyService.getJourney(journeyId).getUserId(), authorization);
        return journeyService.completeJourney(journeyId);
    }

    public record CreateJourneyRequest(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long emberTripId,
            @NotNull @Positive Long originLocationId,
            @NotNull @Positive Long destinationLocationId,
            @NotBlank String originName,
            @NotBlank String destinationName,
            @NotNull LocalDateTime scheduledDeparture,
            @NotNull LocalDateTime scheduledArrival
    ) {}
}
