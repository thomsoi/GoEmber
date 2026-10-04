package com.goember.hackathon.journey;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityNotFoundException;
import com.goember.hackathon.user.UserRepository;

@Service
public class JourneyService {

    private final JourneyRepository journeyRepository;
    private final UserRepository userRepository;

    public JourneyService(JourneyRepository journeyRepository, UserRepository userRepository) {
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
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
        if (!userRepository.existsById(userId)) throw new EntityNotFoundException("User not found: " + userId);
        if (!scheduledArrival.isAfter(scheduledDeparture)) {
            throw new IllegalArgumentException("Arrival must be after departure");
        }
        if (originLocationId.equals(destinationLocationId)) {
            throw new IllegalArgumentException("Origin and destination must differ");
        }
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

    @Transactional
    public Journey startJourney(Long journeyId) {
        Long userId = journeyRepository.findOwnerId(journeyId).orElseThrow(() -> new EntityNotFoundException("Journey not found: " + journeyId));
        lockUser(userId);
        Journey journey = journeyRepository.findForUpdate(journeyId).orElseThrow(EntityNotFoundException::new);
        if (journey.getCompletedAt() != null) throw conflict("Completed journeys cannot be restarted");
        if (journey.isActive()) return journey;
        if (journeyRepository.findByUserIdAndActiveTrue(userId).isPresent()) {
            throw conflict("End the active journey before starting another");
        }

        journey.setStartedAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        journey.setActive(true);

        return journeyRepository.save(journey);
    }

    @Transactional
    public Journey completeJourney(Long journeyId) {
        lockUser(journeyRepository.findOwnerId(journeyId).orElseThrow(() -> new EntityNotFoundException("Journey not found: " + journeyId)));
        Journey journey = journeyRepository.findForUpdate(journeyId).orElseThrow(EntityNotFoundException::new);
        if (journey.getCompletedAt() != null) return journey;
        if (!journey.isActive()) throw conflict("Start the journey before completing it");

        journey.setCompletedAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        journey.setActive(false);

        return journeyRepository.save(journey);
    }

    public Optional<Journey> getActiveJourney(Long userId) {
        return journeyRepository.findByUserIdAndActiveTrue(userId);
    }

    public Journey getJourney(Long journeyId) {
        return journeyRepository.findById(journeyId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Journey not found: " + journeyId
                        )
                );
    }

    private void lockUser(Long userId) {
        userRepository.findForUpdate(userId).orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
