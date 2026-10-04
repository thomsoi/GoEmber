package com.goember.hackathon.passport;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.ember.EmberApiException;
import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.PassportStampRepository;
import com.goember.hackathon.stamp.Stamp;
import com.goember.hackathon.stamp.StampService;
import com.goember.hackathon.stamp.StampRepository;
import com.goember.hackathon.user.User;
import com.goember.hackathon.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PassportService {

	private static final Logger logger = LoggerFactory.getLogger(PassportService.class);

	private final PassportRepository passportRepository;
	private final UserRepository userRepository;
	private final StampService stampService;
	private final StampRepository stampRepository;
	private final PassportStampRepository passportStampRepository;
	private final PassportJourneyRepository passportJourneyRepository;
	private final EmberClient emberClient;
	private final PassportVisitRepository visitRepository;
	private final TransactionTemplate writeTransaction;

	public PassportService(
			PassportRepository passportRepository,
			UserRepository userRepository,
			StampService stampService,
			StampRepository stampRepository,
			PassportStampRepository passportStampRepository,
			PassportJourneyRepository passportJourneyRepository,
			EmberClient emberClient,
			PassportVisitRepository visitRepository,
			PlatformTransactionManager transactionManager) {
		this.passportRepository = passportRepository;
		this.userRepository = userRepository;
		this.stampService = stampService;
		this.stampRepository = stampRepository;
		this.passportStampRepository = passportStampRepository;
		this.passportJourneyRepository = passportJourneyRepository;
		this.emberClient = emberClient;
		this.visitRepository = visitRepository;
		this.writeTransaction = new TransactionTemplate(transactionManager);
	}

	@Transactional
	public Passport getOrCreatePassport(Long userId) {
		lockUser(userId);
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

	public PassportStamp recordLocationVisit(Long userId, long emberLocationId) {
		return recordLocationVisit(userId, emberLocationId, null);
	}

	public PassportStamp recordLocationVisit(Long userId, long emberLocationId, String locationName) {
		return recordLocationVisit(userId, emberLocationId, locationName, null);
	}

	public PassportStamp recordLocationVisit(Long userId, long emberLocationId, String locationName, String operationId) {
		if (emberLocationId <= 0) throw new IllegalArgumentException("Location ID must be positive");
		Stamp stamp = stampService.getOrCreateForLocation(emberLocationId);
		return writeTransaction.execute(status -> {
			lockUser(userId);
			return recordStampVisit(findOrCreatePassport(userId), stamp, operationId);
		});
	}

	public PassportStamp recordTownVisit(Long userId, String townName) {
		return recordTownVisit(userId, townName, null);
	}

	public void recordLocationVisits(Long userId, List<LocationVisit> visits) {
		if (visits == null || visits.isEmpty() || visits.size() > 50 || visits.stream().anyMatch(visit ->
				visit == null || visit.locationId() <= 0 || visit.operationId() == null || visit.operationId().isBlank())) {
			throw new IllegalArgumentException("Provide between 1 and 50 location visits with operation IDs");
		}
		var stamps = stampService.getOrCreateForLocations(visits.stream().map(LocationVisit::locationId).toList());
		writeTransaction.execute(status -> {
			lockUser(userId);
			Passport passport = findOrCreatePassport(userId);
			for (var visit : visits) recordStampVisit(passport, stamps.get(visit.locationId()), visit.operationId());
			return null;
		});
	}

	public record LocationVisit(long locationId, String operationId) {}

	public PassportStamp recordTownVisit(Long userId, String townName, String operationId) {
		Stamp stamp = stampService.getOrCreateForTown(townName);
		return writeTransaction.execute(status -> {
			lockUser(userId);
			return recordStampVisit(findOrCreatePassport(userId), stamp, operationId);
		});
	}

	private PassportStamp recordStampVisit(Passport passport, Stamp stamp, String operationId) {
		if (operationId != null) {
			var prior = visitRepository.findByPassport_PassportIdAndOperationId(passport.getPassportId(), operationId);
			if (prior.isPresent()) {
				if (!prior.get().getStampKey().equals(stamp.getStampKey())) {
					throw new org.springframework.web.server.ResponseStatusException(
							org.springframework.http.HttpStatus.CONFLICT, "Operation ID already used for another stamp");
				}
				return passport.getStamps().stream().filter(entry -> entry.getStamp().getStampKey().equals(stamp.getStampKey()))
						.findFirst().orElseThrow();
			}
		}
		PassportStamp result = passport.getStamps().stream()
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
		if (operationId != null) visitRepository.save(new PassportVisit(passport, operationId, stamp.getStampKey()));
		return result;
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

	public void recordCompletedJourney(
			Long userId,
			String journeyKey,
			String routeNumber,
			long originLocationId,
			long destinationLocationId,
			String originName,
			String destinationName,
			List<String> towns) {
		if (originLocationId <= 0 || destinationLocationId <= 0) {
			throw new IllegalArgumentException("A journey requires positive location IDs");
		}
		if (routeNumber != null && (routeNumber.isBlank() || routeNumber.length() > 32)) {
			throw new IllegalArgumentException("Bus number must contain between 1 and 32 characters");
		}
		String normalizedRouteNumber = routeNumber == null ? null : routeNumber.trim().toUpperCase(Locale.ROOT);
		boolean alreadyRecorded = writeTransaction.execute(status -> {
			lockUser(userId);
			Passport passport = findOrCreatePassport(userId);
			return passportJourneyRepository.findByPassport_PassportIdAndJourneyKey(passport.getPassportId(), journeyKey).isPresent();
		});
		if (alreadyRecorded) return;
		Double distanceKilometers = findDistanceKilometers(originLocationId, destinationLocationId);
		if (distanceKilometers == null) {
			logger.warn(
					"Could not estimate distance for completed journey {} from {} to {}; the bus number will still be recorded",
					journeyKey,
					originName,
					destinationName);
		}

		Set<String> normalizedTowns = towns.stream()
				.map(String::trim)
				.filter(town -> !town.isEmpty())
				.map(town -> town.toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());

		writeTransaction.executeWithoutResult(status -> {
			lockUser(userId);
			Passport passport = findOrCreatePassport(userId);
			if (passportJourneyRepository.findByPassport_PassportIdAndJourneyKey(passport.getPassportId(), journeyKey).isEmpty()) {
				passportJourneyRepository.save(new PassportJourney(passport, journeyKey, normalizedRouteNumber,
						distanceKilometers, normalizedTowns));
			}
		});
	}

	@Transactional(readOnly = true)
	public TravelStats getTravelStats(Long passportId) {
		List<PassportJourney> journeys = passportJourneyRepository.findAllByPassport_PassportId(passportId);
		boolean allDistancesAvailable = journeys.stream()
				.allMatch(journey -> journey.getDistanceKilometers() != null);
		Double totalDistanceKilometers = allDistancesAvailable
				? journeys.stream().mapToDouble(PassportJourney::getDistanceKilometers).sum()
				: null;
		long townsVisited = passportStampRepository.findAllByPassport_PassportId(passportId).stream()
				.map(entry -> entry.getStamp().getRegionName())
				.filter(town -> town != null && !town.isBlank())
				.map(town -> town.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT))
				.distinct()
				.count();
		long busNumbersRidden = journeys.stream().map(PassportJourney::getRouteNumber)
				.filter(number -> number != null && !number.isBlank()).distinct().count();
		return new TravelStats(totalDistanceKilometers, busNumbersRidden, townsVisited);
	}

	private Map<String, Object> findLocation(long locationId) {
		return emberClient.findLocationById(locationId).orElse(null);
	}

	private Double findDistanceKilometers(long originLocationId, long destinationLocationId) {
		var originStamp = stampRepository.findByLocationId(originLocationId);
		var destinationStamp = stampRepository.findByLocationId(destinationLocationId);
		if (originStamp.isPresent() && destinationStamp.isPresent()
				&& originStamp.get().getLatitude() != null && originStamp.get().getLongitude() != null
				&& destinationStamp.get().getLatitude() != null && destinationStamp.get().getLongitude() != null) {
			return distanceInKilometers(originStamp.get().getLatitude(), originStamp.get().getLongitude(),
					destinationStamp.get().getLatitude(), destinationStamp.get().getLongitude());
		}
		Map<String, Object> origin;
		Map<String, Object> destination;
		try {
			origin = findLocation(originLocationId);
			destination = findLocation(destinationLocationId);
		} catch (EmberApiException exception) {
			logger.warn("Could not resolve journey endpoint coordinates from Ember: {}", exception.getMessage());
			return null;
		}
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

	private Double getCoordinate(Map<String, Object> location, String coordinateName) {
		Object value = location.get(coordinateName);
		if (value instanceof Number number) {
			double coordinate = number.doubleValue();
			double max = coordinateName.equals("lat") ? 90 : 180;
			if (Double.isFinite(coordinate) && Math.abs(coordinate) <= max) {
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
		haversine = Math.max(0, Math.min(1, haversine));
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

	private void lockUser(Long userId) {
		userRepository.findForUpdate(userId).orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
	}

	public record TravelStats(Double totalDistanceKilometers, long busNumbersRidden, long townsVisited) {
	}
}
