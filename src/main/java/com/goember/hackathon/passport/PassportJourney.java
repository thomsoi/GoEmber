package com.goember.hackathon.passport;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_passport_journey_key",
        columnNames = {"passportId", "journeyKey"}))
public class PassportJourney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passportId", nullable = false, updatable = false)
    private Passport passport;

    @Column(name = "journeyKey", nullable = false, updatable = false)
    private String journeyKey;

    @Column(nullable = false, updatable = false)
    private Double distanceKilometers;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "passport_journey_towns",
            joinColumns = @JoinColumn(name = "passportJourneyId"))
    @Column(name = "town", nullable = false)
    private Set<String> towns = new HashSet<>();

    protected PassportJourney() {
    }

    public PassportJourney(
            Passport passport,
            String journeyKey,
            Double distanceKilometers,
            Set<String> towns) {
        this.passport = passport;
        this.journeyKey = journeyKey;
        this.distanceKilometers = distanceKilometers;
        this.towns = new HashSet<>(towns);
    }

    public Double getDistanceKilometers() {
        return distanceKilometers;
    }

    public Set<String> getTowns() {
        return towns;
    }
}
