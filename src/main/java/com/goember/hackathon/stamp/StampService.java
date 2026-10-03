package com.goember.hackathon.stamp;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goember.hackathon.ember.EmberClient;

import jakarta.persistence.EntityNotFoundException;

@Service
public class StampService {

	private final StampRepository stampRepository;
	private final EmberClient emberClient;

	public StampService(StampRepository stampRepository, EmberClient emberClient) {
		this.stampRepository = stampRepository;
		this.emberClient = emberClient;
	}

	@Transactional
	public Stamp getOrCreateForLocation(long emberLocationId) {
		return stampRepository.findByLocationId(emberLocationId)
				.orElseGet(() -> createFromEmberLocation(emberLocationId));
	}

	private Stamp createFromEmberLocation(long emberLocationId) {
		Map location = emberClient.findLocationById(emberLocationId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Ember location not found: " + emberLocationId));
		String locationName = Objects.toString(location.get("name"), null);
		if (locationName == null || locationName.isBlank()) {
			throw new IllegalStateException("Ember location has no name: " + emberLocationId);
		}

		return stampRepository.save(new Stamp(emberLocationId, locationName));
	}
}
