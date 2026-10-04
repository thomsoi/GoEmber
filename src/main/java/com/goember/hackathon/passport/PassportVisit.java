package com.goember.hackathon.passport;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"passport_id", "operation_id"}))
public class PassportVisit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passport_id", nullable = false)
    private Passport passport;
    @Column(name = "operation_id", nullable = false, updatable = false, length = 100)
    private String operationId;
    @Column(nullable = false, updatable = false)
    private String stampKey;

    protected PassportVisit() {}
    public PassportVisit(Passport passport, String operationId, String stampKey) {
        this.passport = passport;
        this.operationId = operationId;
        this.stampKey = stampKey;
    }
    public String getStampKey() { return stampKey; }
}
