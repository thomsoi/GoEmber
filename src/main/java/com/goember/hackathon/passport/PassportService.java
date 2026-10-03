package com.goember.hackathon.passport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.Stamp;
import com.goember.hackathon.stamp.StampService;
import com.goember.hackathon.user.User;
import com.goember.hackathon.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PassportService {

	private final PassportRepository passportRepository;
	private final UserRepository userRepository;
	private final StampService stampService;

	public PassportService(
			PassportRepository passportRepository,
			UserRepository userRepository,
			StampService stampService) {
		this.passportRepository = passportRepository;
		this.userRepository = userRepository;
		this.stampService = stampService;
	}

	@Transactional
	public Passport getOrCreatePassport(Long userId) {
		return findOrCreatePassport(userId);
	}

	@Transactional
	public PassportStamp recordLocationVisit(Long userId, long emberLocationId) {
		Passport passport = findOrCreatePassport(userId);
		Stamp stamp = stampService.getOrCreateForLocation(emberLocationId);

		PassportStamp passportStamp = passport.getStamps().stream()
				.filter(entry -> entry.getStamp().getLocationId().equals(emberLocationId))
				.findFirst()
				.orElse(null);

		if (passportStamp == null) {
			return passport.addStamp(stamp);
		}

		passportStamp.recordVisit();
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
