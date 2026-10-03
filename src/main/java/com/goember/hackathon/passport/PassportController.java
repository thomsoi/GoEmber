package com.goember.hackathon.passport;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.Stamp;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

	@PostMapping("/{userId}/journeys")
	public void recordCompletedJourney(
			@PathVariable Long userId,
			@Valid @RequestBody CompletedJourneyRequest request) {
		passportService.recordCompletedJourney(
				userId,
				request.journeyKey(),
				request.originLocationId(),
				request.destinationLocationId(),
				request.originName(),
				request.destinationName(),
				request.towns());
	}

	@PostMapping("/{userId}/locations/{emberLocationId}/visits")
	public StampVisitResponse recordLocationVisit(
			@PathVariable Long userId,
			@PathVariable long emberLocationId,
			@RequestBody(required = false) LocationVisitRequest request) {
		try {
			String locationName = request == null ? null : request.locationName();
			PassportStamp passportStamp = passportService.recordLocationVisit(userId, emberLocationId, locationName);
			Stamp stamp = passportStamp.getStamp();
			return new StampVisitResponse(
					stamp.getLocationId(),
					stamp.getStampKey(),
					stamp.getName(),
					passportStamp.getTier().getValue(),
					passportStamp.getVisitCount(),
					passportService.percentOfUsersWithStamp(stamp.getLocationId()),
					passportStamp.getVisitCount() > 1,
					passportStamp.getFirstVisitedAt(),
					passportStamp.getMostRecentVisitAt());
		} catch (EntityNotFoundException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
		}
	}

	@PostMapping("/{userId}/towns/visits")
	public StampVisitResponse recordTownVisit(
			@PathVariable Long userId,
			@Valid @RequestBody TownVisitRequest request) {
		PassportStamp passportStamp = passportService.recordTownVisit(userId, request.townName());
		Stamp stamp = passportStamp.getStamp();
		return new StampVisitResponse(
				stamp.getLocationId(),
				stamp.getStampKey(),
				stamp.getName(),
				passportStamp.getTier().getValue(),
				passportStamp.getVisitCount(),
				passportService.percentOfUsersWithStampKey(stamp.getStampKey()),
				passportStamp.getVisitCount() > 1,
				passportStamp.getFirstVisitedAt(),
				passportStamp.getMostRecentVisitAt());
	}

	private PassportResponse toPassportResponse(Passport passport) {
		List<PassportStampResponse> stamps = passport.getStamps().stream()
				.map(passportStamp -> new PassportStampResponse(
						passportStamp.getStamp().getLocationId(),
						passportStamp.getStamp().getStampKey(),
						passportStamp.getStamp().getName(),
						passportStamp.getTier().getValue(),
						passportStamp.getVisitCount(),
						passportStamp.getStamp().getLocationId() == null
								? passportService.percentOfUsersWithStampKey(passportStamp.getStamp().getStampKey())
								: passportService.percentOfUsersWithStamp(passportStamp.getStamp().getLocationId()),
						passportStamp.getFirstVisitedAt(),
						passportStamp.getMostRecentVisitAt()))
				.toList();

		PassportService.TravelStats travelStats = passportService.getTravelStats(passport.getPassportId());
		return new PassportResponse(
				passport.getPassportId(),
				travelStats.totalDistanceKilometers(),
				travelStats.routesTravelled(),
				travelStats.townsVisited(),
				stamps);
	}

	public record PassportResponse(
			Long passportId,
			Double totalDistanceTravelled,
			long routesTravelled,
			long townsVisited,
			List<PassportStampResponse> stamps) {
	}

	public record PassportStampResponse(
			Long locationId,
			String stampKey,
			String stampName,
			String tier,
			int visitCount,
			double percentOfUsersWithStamp,
			Instant firstVisitedAt,
			Instant mostRecentVisitAt) {
	}

	public record StampVisitResponse(
			Long locationId,
			String stampKey,
			String stampName,
			String tier,
			int visitCount,
			double percentOfUsersWithStamp,
			boolean alreadyVisited,
			Instant firstVisitedAt,
			Instant mostRecentVisitAt) {
	}

	public record LocationVisitRequest(String locationName) {
	}

	public record TownVisitRequest(@NotBlank String townName) {
	}

	public record CompletedJourneyRequest(
			@NotBlank String journeyKey,
			@NotNull @Positive Long originLocationId,
			@NotNull @Positive Long destinationLocationId,
			@NotBlank String originName,
			@NotBlank String destinationName,
			@NotEmpty List<@NotBlank String> towns) {
	}
}
