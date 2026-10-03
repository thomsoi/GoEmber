package com.goember.hackathon.passport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.PassportStampRepository;
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
	private final PassportStampRepository passportStampRepository;

	public PassportService(
			PassportRepository passportRepository,
			UserRepository userRepository,
			StampService stampService,
			PassportStampRepository passportStampRepository) {
		this.passportRepository = passportRepository;
		this.userRepository = userRepository;
		this.stampService = stampService;
		this.passportStampRepository = passportStampRepository;
	}

	@Transactional
	public Passport getOrCreatePassport(Long userId) {
		return findOrCreatePassport(userId);
	}

	@Transactional(readOnly = true)
	public double percentOfUsersWithStamp(Long locationId) {
		long totalUsers = userRepository.count();
		if (totalUsers == 0) {
			return 0.0;
		}

		long stampOwners = passportStampRepository.countByStamp_LocationId(locationId);
		return stampOwners * 100.0 / totalUsers;
	}

	@Transactional
	public PassportStamp recordLocationVisit(Long userId, long emberLocationId) {
		return recordLocationVisit(userId, emberLocationId, null);
	}

	@Transactional
	public PassportStamp recordLocationVisit(Long userId, long emberLocationId, String locationName) {
		Passport passport = findOrCreatePassport(userId);
		Stamp stamp = locationName == null || locationName.isBlank()
				? stampService.getOrCreateForLocation(emberLocationId)
				: stampService.getOrCreateForLocation(emberLocationId, locationName);

		PassportStamp passportStamp = passport.getStamps().stream()
				.filter(entry -> entry.getStamp().getLocationId().equals(emberLocationId))
				.findFirst()
				.orElseGet(() -> {
					PassportStamp createdStamp = passport.addStamp(stamp);
					passportRepository.save(passport);
					return createdStamp;
				});

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
