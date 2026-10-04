package com.goember.hackathon.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int streak;
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String credentialHash;
    @Transient
    private String accessToken;

    public User() {
    }

    public User(String name) {
        this.name = name;
        this.streak = 0;
        byte[] secret = new byte[32];
        new java.security.SecureRandom().nextBytes(secret);
        accessToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        credentialHash = GuestAccess.hash(accessToken);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    @JsonIgnore
    public String getCredentialHash() { return credentialHash; }
    @JsonIgnore
    public String getAccessToken() { return accessToken; }
}
