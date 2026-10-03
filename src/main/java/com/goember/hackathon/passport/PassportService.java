package com.goember.hackathon.passport;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.PassportStampRepository;
import com.goember.hackathon.stamp.Stamp;
import com.goember.hackathon.stamp.StampService;
import com.goember.hackathon.user.User;
import com.goember.hackathon.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PassportService {

	private static final Logger logger = LoggerFactory.getLogger(PassportService.class);

	private final PassportRepository passportRepository;
	private final UserRepository userRepository;
	private final StampService stampService;
	private final PassportStampRepository passportStampRepository;
	private final PassportJourneyRepository passportJourneyRepository;
	private final EmberClient emberClient;

	public PassportService(
			PassportRepository passportRepository,
			UserRepository userRepository,
			StampService stampService,
			PassportStampRepository passportStampRepository,
			PassportJourneyRepository passportJourneyRepository,
			EmberClient emberClient) {
		this.passportRepository = passportRepository;
		this.userRepository = userRepository;
		this.stampService = stampService;
		this.passportStampRepository = passportStampRepository;
		this.passportJourneyRepository = passportJourneyRepository;
		this.emberClient = emberClient;
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

		return recordStampVisit(passport, stamp);
	}

	@Transactional
	public PassportStamp recordTownVisit(Long userId, String townName) {
		Passport passport = findOrCreatePassport(userId);
		Stamp stamp = stampService.getOrCreateForTown(townName);
		return recordStampVisit(passport, stamp);
	}

	private PassportStamp recordStampVisit(Passport passport, Stamp stamp) {
		return passport.getStamps().stream()
				.filter(entry -> entry.getStamp().getStampKey().equals(stamp.getStampKey()))
				.findFirst()
				.map(passportStamp -> {
					passportStamp.recordVisit();
					return passportStamp;
				})
				.orElseGet(() -> {
					PassportStamp createdStamp = passport.addStamp(stamp);
					passportRepository.save(passport);
					return createdStamp;
				});
	}

	@Transactional(readOnly = true)
	public double percentOfUsersWithStampKey(String stampKey) {
		long totalUsers = userRepository.count();
		if (totalUsers == 0) {
			return 0.0;
		}

		long stampOwners = passportStampRepository.countByStamp_StampKey(stampKey);
		return stampOwners * 100.0 / totalUsers;
	}

	@Transactional
	public void recordCompletedJourney(
			Long userId,
			String journeyKey,
			long originLocationId,
			long destinationLocationId,
			String originName,
			String destinationName,
			List<String> towns) {
		Passport passport = findOrCreatePassport(userId);
		if (passportJourneyRepository
				.findByPassport_PassportIdAndJourneyKey(passport.getPassportId(), journeyKey)
				.isPresent()) {
			return;
		}

		Double distanceKilometers = findDistanceKilometers(
				originLocationId,
				destinationLocationId,
				originName,
				destinationName);
		if (distanceKilometers == null) {
			logger.warn(
					"Could not estimate distance for completed journey {} from {} to {}; route and town totals will still be recorded",
					journeyKey,
					originName,
					destinationName);
		}

		Set<String> normalizedTowns = towns.stream()
				.map(String::trim)
				.filter(town -> !town.isEmpty())
				.map(town -> town.toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());

		passportJourneyRepository.save(new PassportJourney(
				passport,
				journeyKey,
				distanceKilometers,
				normalizedTowns));
	}

	@Transactional(readOnly = true)
	public TravelStats getTravelStats(Long passportId) {
		List<PassportJourney> journeys = passportJourneyRepository.findAllByPassport_PassportId(passportId);
		boolean allDistancesAvailable = journeys.stream()
				.allMatch(journey -> journey.getDistanceKilometers() != null);
		Double totalDistanceKilometers = allDistancesAvailable
				? journeys.stream().mapToDouble(PassportJourney::getDistanceKilometers).sum()
				: null;
		long townsVisited = journeys.stream()
				.flatMap(journey -> journey.getTowns().stream())
				.distinct()
				.count();

		return new TravelStats(totalDistanceKilometers, journeys.size(), townsVisited);
	}

	private Map<String, Object> findLocation(long locationId, String locationName) {
		Map<String, Object> location = emberClient.findLocationById(locationId).orElse(null);
		if (location != null || locationName == null || locationName.isBlank()) {
			return location;
		}

		List<Map<String, Object>> matches = emberClient.searchLocations(locationName, 50, "STOP_POINT");
		if (matches == null) {
			return null;
		}

		String normalizedName = locationName.trim().toLowerCase(Locale.ROOT);
		return matches.stream()
				.filter(Objects::nonNull)
				.filter(candidate -> normalizedName.equals(normalize(candidate.get("name")))
						|| normalizedName.equals(normalize(candidate.get("region_name"))))
				.findFirst()
				.orElse(null);
	}

	private Double findDistanceKilometers(
			long originLocationId,
			long destinationLocationId,
			String originName,
			String destinationName) {
		Map<String, Object> origin = findLocation(originLocationId, originName);
		Map<String, Object> destination = findLocation(destinationLocationId, destinationName);
		if (origin == null || destination == null) {
			return null;
		}

		Double originLatitude = getCoordinate(origin, "lat");
		Double originLongitude = getCoordinate(origin, "lon");
		Double destinationLatitude = getCoordinate(destination, "lat");
		Double destinationLongitude = getCoordinate(destination, "lon");
		if (originLatitude == null || originLongitude == null
				|| destinationLatitude == null || destinationLongitude == null) {
			return null;
		}

		return distanceInKilometers(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
	}

	private String normalize(Object value) {
		return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
	}

	private Double getCoordinate(Map<String, Object> location, String coordinateName) {
		Object value = location.get(coordinateName);
		if (value instanceof Number number) {
			double coordinate = number.doubleValue();
			if (Double.isFinite(coordinate)) {
				return coordinate;
			}
		}
		return null;
	}

	private double distanceInKilometers(double latitude1, double longitude1, double latitude2, double longitude2) {
		double earthRadiusKilometers = 6371.0;
		double latitudeDifference = Math.toRadians(latitude2 - latitude1);
		double longitudeDifference = Math.toRadians(longitude2 - longitude1);
		double haversine = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
				+ Math.cos(Math.toRadians(latitude1))
				* Math.cos(Math.toRadians(latitude2))
				* Math.sin(longitudeDifference / 2)
				* Math.sin(longitudeDifference / 2);
		return earthRadiusKilometers * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
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

	public record TravelStats(Double totalDistanceKilometers, long routesTravelled, long townsVisited) {
	}
}
