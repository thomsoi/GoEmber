package com.goember.hackathon.journey;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class JourneyService {

    private final JourneyRepository journeyRepository;

    public JourneyService(JourneyRepository journeyRepository) {
        this.journeyRepository = journeyRepository;
    }

    public Journey createJourney(
            Long userId,
            Long emberTripId,
            Long originLocationId,
            Long destinationLocationId,
            String originName,
            String destinationName,
            LocalDateTime scheduledDeparture,
            LocalDateTime scheduledArrival
    ) {
        Journey journey = new Journey(
                userId,
                emberTripId,
                originLocationId,
                destinationLocationId,
                originName,
                destinationName,
                scheduledDeparture,
                scheduledArrival
        );

        return journeyRepository.save(journey);
    }

    public Journey startJourney(Long journeyId) {

        Journey journey = getJourney(journeyId);

        journey.setStartedAt(LocalDateTime.now());
        journey.setActive(true);

        return journeyRepository.save(journey);
    }

    public Journey completeJourney(Long journeyId) {

        Journey journey = getJourney(journeyId);

        journey.setCompletedAt(LocalDateTime.now());
        journey.setActive(false);

        return journeyRepository.save(journey);
    }

    public Optional<Journey> getActiveJourney(Long userId) {
        return journeyRepository.findByUserIdAndActiveTrue(userId);
    }

    public Journey getJourney(Long journeyId) {
        return journeyRepository.findById(journeyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Journey not found: " + journeyId
                        )
                );
    }
}
