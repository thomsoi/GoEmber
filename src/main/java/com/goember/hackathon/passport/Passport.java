package com.goember.hackathon.passport;

import java.util.ArrayList;
import java.util.List;

import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.Stamp;
import com.goember.hackathon.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Passport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passportId", nullable = false, updatable = false)
    private Long passportId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, updatable = false)
    private User user;

    @Column(name= "totalDistanceTravelled", nullable = false)
    private float totalDistance;

    @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PassportStamp> stamps = new ArrayList<>();

    public Passport() {
    }

    public Passport(User user) {
        this.user = user;
    }

    public Long getPassportId() {
        return passportId;
    }

    public User getUser() {
        return user;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public List<PassportStamp> getStamps() {
        return stamps;
    }

    public PassportStamp addStamp(Stamp stamp) {
        PassportStamp passportStamp = new PassportStamp(this, stamp);
        stamps.add(passportStamp);
        return passportStamp;
    }

    public void removeStamp(PassportStamp stamp) {
        stamps.remove(stamp);
    }

}