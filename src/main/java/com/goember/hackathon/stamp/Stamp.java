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

	@Column(length = 100)
	private String regionName;

	private Double latitude;

	private Double longitude;

	public Stamp() {
	}

	public Stamp(Long locationId, String name) {
		this.locationId = locationId;
		this.stampKey = "location:" + locationId;
		this.name = name;
	}

	public Stamp(Long locationId, String name, String regionName, Double latitude, Double longitude) {
		this(locationId, name);
		this.regionName = regionName;
		this.latitude = latitude;
		this.longitude = longitude;
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

	public String getRegionName() {
		return regionName;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public void setName(String name) {
		this.name = name;
	}
}
