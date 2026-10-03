package com.goember.hackathon.passport;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.Stamp;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/passports")
public class PassportController {

	private final PassportService passportService;

	public PassportController(PassportService passportService) {
		this.passportService = passportService;
	}

	@GetMapping("/{userId}")
	public PassportResponse getPassport(@PathVariable Long userId) {
		try {
			return toPassportResponse(passportService.getOrCreatePassport(userId));
		} catch (EntityNotFoundException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
		}
	}

	@PostMapping("/{userId}/locations/{emberLocationId}/visits")
	public StampVisitResponse recordLocationVisit(
			@PathVariable Long userId,
			@PathVariable long emberLocationId) {
		try {
			PassportStamp passportStamp = passportService.recordLocationVisit(userId, emberLocationId);
			Stamp stamp = passportStamp.getStamp();
			return new StampVisitResponse(
					stamp.getLocationId(),
					stamp.getName(),
					passportStamp.getTier(),
					passportStamp.getVisitCount(),
					passportStamp.getVisitCount() > 1,
					passportStamp.getFirstVisitedAt(),
					passportStamp.getMostRecentVisitAt());
		} catch (EntityNotFoundException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
		}
	}

	private PassportResponse toPassportResponse(Passport passport) {
		List<PassportStampResponse> stamps = passport.getStamps().stream()
				.map(passportStamp -> new PassportStampResponse(
						passportStamp.getStamp().getLocationId(),
						passportStamp.getStamp().getName(),
						passportStamp.getTier(),
						passportStamp.getVisitCount(),
						passportStamp.getFirstVisitedAt(),
						passportStamp.getMostRecentVisitAt()))
				.toList();

		return new PassportResponse(passport.getPassportId(), passport.getTotalDistance(), stamps);
	}

	public record PassportResponse(
			UUID passportId,
			float totalDistanceTravelled,
			List<PassportStampResponse> stamps) {
	}

	public record PassportStampResponse(
			Long locationId,
			String stampName,
			int tier,
			int visitCount,
			Instant firstVisitedAt,
			Instant mostRecentVisitAt) {
	}

	public record StampVisitResponse(
			Long locationId,
			String stampName,
			int tier,
			int visitCount,
			boolean alreadyVisited,
			Instant firstVisitedAt,
			Instant mostRecentVisitAt) {
	}
}
