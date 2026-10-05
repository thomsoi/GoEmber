package com.goember.hackathon.stamp;

import java.time.Instant;

import com.goember.hackathon.passport.Passport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "uk_passport_stamp",
        columnNames = {"passportId", "stampId"}))
public class PassportStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passportStampId", nullable = false, updatable = false)
    private Long passportStampId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passportId", nullable = false, updatable = false)
    private Passport passport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stampId", nullable = false, updatable = false)
    private Stamp stamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StampTier tier = StampTier.UNTIERED;

    @Column(nullable = false)
    private int visitCount = 1;

    @Column(nullable = false, updatable = false)
    private Instant firstVisitedAt;

    @Column(nullable = false)
    private Instant mostRecentVisitAt;

    public PassportStamp() {
    }

    public PassportStamp(Passport passport, Stamp stamp) {
        this.passport = passport;
        this.stamp = stamp;
        this.firstVisitedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.mostRecentVisitAt = firstVisitedAt;
        this.tier = StampTier.forVisitCount(1);
    }

    public Long getPassportStampId() {
        return passportStampId;
    }

    public Passport getPassport() {
        return passport;
    }

    public Stamp getStamp() {
        return stamp;
    }

    public StampTier getTier() {
        return tier;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public Instant getFirstVisitedAt() {
        return firstVisitedAt;
    }

    public Instant getMostRecentVisitAt() {
        return mostRecentVisitAt;
    }

    public void recordVisit() {
        visitCount++;
        mostRecentVisitAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        tier = StampTier.forVisitCount(visitCount);
    }
}
