package com.goember.hackathon.passport;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.Stamp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.RequestHeader;
import com.goember.hackathon.user.GuestAccess;

@RestController
@RequestMapping("/api/passports")
public class PassportController {

	private final PassportService passportService;
	private final GuestAccess access;

	public PassportController(PassportService passportService, GuestAccess access) {
		this.passportService = passportService;
		this.access = access;
	}

	@GetMapping("/{userId}")
	public PassportResponse getPassport(@PathVariable Long userId,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		access.requireUser(userId, authorization);
		return toPassportResponse(passportService.getOrCreatePassport(userId));
	}

	@PostMapping("/{userId}/journeys")
	public void recordCompletedJourney(
			@PathVariable Long userId,
			@Valid @RequestBody CompletedJourneyRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		access.requireUser(userId, authorization);
		passportService.recordCompletedJourney(
				userId,
				request.journeyKey(),
				request.routeNumber(),
				request.originLocationId(),
				request.destinationLocationId(),
				request.originName(),
				request.destinationName(),
				request.towns(),
				request.visitedLocationIds());
	}

	@PostMapping("/{userId}/locations/{emberLocationId}/visits")
	public StampVisitResponse recordLocationVisit(
			@PathVariable Long userId,
			@PathVariable long emberLocationId,
			@Valid @RequestBody LocationVisitRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		access.requireUser(userId, authorization);
		PassportStamp passportStamp = passportService.recordLocationVisit(
				userId, emberLocationId, request.locationName(), request.operationId());
		return toStampVisitResponse(passportStamp);
	}

	@PostMapping("/{userId}/locations/visits")
	public void recordLocationVisits(@PathVariable Long userId,
			@Valid @RequestBody LocationVisitsRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		access.requireUser(userId, authorization);
		passportService.recordLocationVisits(userId, request.visits().stream()
				.map(visit -> new PassportService.LocationVisit(visit.locationId(), visit.operationId())).toList());
	}

	@PostMapping("/{userId}/towns/visits")
	public StampVisitResponse recordTownVisit(
			@PathVariable Long userId,
			@Valid @RequestBody TownVisitRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		access.requireUser(userId, authorization);
		PassportStamp passportStamp = passportService.recordTownVisit(userId, request.townName(), request.operationId());
		return toStampVisitResponse(passportStamp);
	}

	private StampVisitResponse toStampVisitResponse(PassportStamp passportStamp) {
		Stamp stamp = passportStamp.getStamp();
		return new StampVisitResponse(
				stamp.getLocationId(),
				stamp.getStampKey(),
				stamp.getName(),
				passportStamp.getTier().getValue(),
				passportStamp.getVisitCount(),
				stamp.getLocationId() == null
						? passportService.percentOfUsersWithStampKey(stamp.getStampKey())
						: passportService.percentOfUsersWithStamp(stamp.getLocationId()),
				passportStamp.getVisitCount() > 1,
				passportStamp.getFirstVisitedAt(),
				passportStamp.getMostRecentVisitAt());
	}

	private PassportResponse toPassportResponse(Passport passport) {
		List<PassportStampResponse> stamps = passport.getStamps().stream()
				.filter(entry -> entry.getStamp().getLocationId() != null)
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
				travelStats.busNumbersRidden(),
				travelStats.townsVisited(),
				stamps,
				travelStats.cities(),
				travelStats.busNumbers(),
				travelStats.buses());
	}

	public record PassportResponse(
			Long passportId,
			Double totalDistanceTravelled,
			long busNumbersRidden,
			long townsVisited,
			List<PassportStampResponse> stamps,
			List<PassportService.VisitedCity> cities,
			List<String> busNumbers,
			List<PassportService.RiddenBus> buses) {
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

	public record LocationVisitRequest(String locationName, @NotBlank @Size(max = 100) String operationId) {
	}

	public record LocationVisitsRequest(@NotEmpty @Size(max = 50) List<@Valid LocationVisitItem> visits) {}
	public record LocationVisitItem(@NotNull @Positive Long locationId,
			@NotBlank @Size(max = 100) String operationId) {}

	public record TownVisitRequest(@NotBlank @Size(max = 100) String townName,
			@NotBlank @Size(max = 100) String operationId) {
	}

	public record CompletedJourneyRequest(
			@NotBlank @Size(max = 100) String journeyKey,
			@Size(max = 32) String routeNumber,
			@NotNull @Positive Long originLocationId,
			@NotNull @Positive Long destinationLocationId,
			@NotBlank @Size(max = 255) String originName,
			@NotBlank @Size(max = 255) String destinationName,
			@NotNull @Size(max = 200) List<@NotBlank @Size(max = 100) String> towns,
			@Size(min = 1, max = 200) List<@NotNull @Positive Long> visitedLocationIds) {
	}
}
