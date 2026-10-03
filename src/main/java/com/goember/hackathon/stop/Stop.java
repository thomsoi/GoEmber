package com.goember.hackathon.stop;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private Long emberLocationId; // id provided by the Ember API

    private String name; // name of the bus stop

    private String type; // STOP_AREA, STOP_POINT or HUB
    // STOP_AREA is the group of all boarding points that belong to one transport location
    // so this includes all of the different stances at a bus station
    // The STOP_POINT is the specific point/bus stop

    private Long areaId; // If STOP_POINT, links to STOP_AREA

    private double latitude;

    private double longitude;

    public Stop() {
    }

    public Stop(
            Long emberLocationId,
            String name,
            String type,
            Long areaId,
            double latitude,
            double longitude
    ) {
        this.emberLocationId = emberLocationId;
        this.name = name;
        this.type = type;
        this.areaId = areaId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public UUID getId() {
        return id;
    }

    public Long getEmberLocationId() {
        return this.emberLocationId;
    }

    public void setEmberLocationId(Long emberLocationId) {
        this.emberLocationId = emberLocationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}