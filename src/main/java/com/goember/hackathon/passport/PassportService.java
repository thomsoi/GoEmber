package com.goember.hackathon.passport;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.Stamp;
import com.goember.hackathon.stamp.StampRepository;
import com.goember.hackathon.user.User;
import com.goember.hackathon.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PassportService {

	private final PassportRepository passportRepository;
	private final UserRepository userRepository;
	private final StampRepository stampRepository;
	private final EmberClient emberClient;

	public PassportService(
			PassportRepository passportRepository,
			UserRepository userRepository,
			StampRepository stampRepository,
			EmberClient emberClient) {
		this.passportRepository = passportRepository;
		this.userRepository = userRepository;
		this.stampRepository = stampRepository;
		this.emberClient = emberClient;
	}

	@Transactional
	public Passport getOrCreatePassport(Long userId) {
		return findOrCreatePassport(userId);
	}

	@Transactional
	public PassportStamp recordLocationVisit(Long userId, long emberLocationId) {
		Map location = emberClient.findLocationById(emberLocationId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Ember location not found: " + emberLocationId));
		String locationName = Objects.toString(location.get("name"), null);
		if (locationName == null || locationName.isBlank()) {
			throw new IllegalStateException("Ember location has no name: " + emberLocationId);
		}

		Passport passport = findOrCreatePassport(userId);
		Stamp stamp = stampRepository.findByLocationId(emberLocationId)
				.orElseGet(() -> stampRepository.save(new Stamp(emberLocationId, locationName)));

		PassportStamp passportStamp = passport.getStamps().stream()
				.filter(entry -> entry.getStamp().getLocationId().equals(emberLocationId))
				.findFirst()
				.orElse(null);

		if (passportStamp == null) {
			return passport.addStamp(stamp);
		}

		passportStamp.recordVisit();
		passportStamp.setTier(passportStamp.getVisitCount());
		return passportStamp;
	}

	private Passport findOrCreatePassport(Long userId) {
		return passportRepository.findByUser_Id(userId)
				.orElseGet(() -> {
					User user = userRepository.findById(userId)
							.orElseThrow(() -> new EntityNotFoundException(
									"User not found: " + userId));
					return passportRepository.save(new Passport(user));
				});
	}
}
