package com.goember.hackathon.stamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Stamp {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stampId", nullable = false, updatable = false)
	private Long stampId;

	@Column(name = "locationId", unique = true, updatable = false)
	private Long locationId;

	@Column(name = "stampKey", nullable = false, unique = true, updatable = false)
	private String stampKey;

	@Column(nullable = false)
	private String name;

	public Stamp() {
	}

	public Stamp(Long locationId, String name) {
		this.locationId = locationId;
		this.stampKey = "location:" + locationId;
		this.name = name;
	}

	public Stamp(String stampKey, String name) {
		this.stampKey = stampKey;
		this.name = name;
	}

	public Long getStampId() {
		return stampId;
	}

	public Long getLocationId() {
		return locationId;
	}

	public String getStampKey() {
		return stampKey;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
