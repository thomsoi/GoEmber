package com.goember.hackathon.journey;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Journey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long emberTripId; // lets us fetch the route from the API

    private Long originLocationId;
    private Long destinationLocationId;

    private String originName;
    private String destinationName;

    private LocalDateTime scheduledDeparture;
    private LocalDateTime scheduledArrival;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private boolean active;

    public Journey() {
    }

    public Journey(
            Long userId,
            Long emberTripId,
            Long originLocationId,
            Long destinationLocationId,
            String originName,
            String destinationName,
            LocalDateTime scheduledDeparture,
            LocalDateTime scheduledArrival
    ) {
        this.userId = userId;
        this.emberTripId = emberTripId;
        this.originLocationId = originLocationId;
        this.destinationLocationId = destinationLocationId;
        this.originName = originName;
        this.destinationName = destinationName;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getEmberTripId() {
        return emberTripId;
    }

    public Long getOriginLocationId() {
        return originLocationId;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public String getOriginName() {
        return originName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public LocalDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public LocalDateTime getScheduledArrival() {
        return scheduledArrival;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
