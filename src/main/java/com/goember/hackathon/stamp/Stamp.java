package com.goember.hackathon.stamp;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Stamp {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "stampId", nullable = false, updatable = false)
	private UUID stampId;

	@Column(name = "locationId", nullable = false, unique = true, updatable = false)
	private String locationId;

	@Column(nullable = false)
	private String name;

	public Stamp() {
	}

	public Stamp(String locationId, String name) {
		this.locationId = locationId;
		this.name = name;
	}

	public UUID getStampId() {
		return stampId;
	}

	public String getLocationId() {
		return locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
