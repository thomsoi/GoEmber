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

import com.goember.hackathon.geo.GeoDistance;
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
	private static final java.util.regex.Pattern RIDE_OPERATION = java.util.regex.Pattern.compile(
			"^(.+):(?:stop:\\d+(?::\\d+)?|town:\\d+)$");

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
		recordCompletedJourney(userId, journeyKey, routeNumber, originLocationId, destinationLocationId,
				originName, destinationName, towns, null);
	}

	public void recordCompletedJourney(Long userId, String journeyKey, String routeNumber,
			long originLocationId, long destinationLocationId, String originName, String destinationName,
			List<String> towns, List<Long> visitedLocationIds) {
		if (originLocationId <= 0 || destinationLocationId <= 0) {
			throw new IllegalArgumentException("A journey requires positive location IDs");
		}
		if (routeNumber != null && (routeNumber.isBlank() || routeNumber.length() > 32)) {
			throw new IllegalArgumentException("Bus number must contain between 1 and 32 characters");
		}
		String normalizedRouteNumber = routeNumber == null ? null : routeNumber.trim().toUpperCase(Locale.ROOT);
		if (visitedLocationIds != null && (visitedLocationIds.isEmpty() || visitedLocationIds.size() > 200
				|| visitedLocationIds.stream().anyMatch(id -> id == null || id <= 0)
				|| visitedLocationIds.getFirst() != originLocationId
				|| visitedLocationIds.getLast() != destinationLocationId)) {
			throw new IllegalArgumentException("Visited stops must be ordered from boarding to alighting, with 1 to 200 positive location IDs");
		}
		boolean alreadyRecorded = writeTransaction.execute(status -> {
			lockUser(userId);
			Passport passport = findOrCreatePassport(userId);
			return passportJourneyRepository.findByPassport_PassportIdAndJourneyKey(passport.getPassportId(), journeyKey).isPresent();
		});
		if (alreadyRecorded) return;
		// Old queued completions contain only endpoints. Never invent their missing route.
		Double distanceKilometers = visitedLocationIds == null
				? findDistanceKilometers(originLocationId, destinationLocationId)
				: findRouteDistanceKilometers(visitedLocationIds);
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
		var cityNames = new java.util.TreeMap<String, String>();
		var cityVisits = new java.util.HashMap<String, Set<String>>();
		var visitsByStamp = visitRepository.findAllByPassport_PassportId(passportId).stream()
				.collect(Collectors.groupingBy(PassportVisit::getStampKey));
		for (var entry : passportStampRepository.findAllByPassport_PassportId(passportId)) {
			// Resolve cities from source metadata, including stamps saved by older versions.
			String name = CityNames.canonical(entry.getStamp().getRegionName());
			if (name == null) continue;
			String key = name.toLowerCase(Locale.ROOT);
			cityNames.putIfAbsent(key, name);
			Set<String> visits = cityVisits.computeIfAbsent(key, ignored -> new java.util.HashSet<>());
			String stampKey = entry.getStamp().getStampKey();
			var recorded = visitsByStamp.getOrDefault(stampKey, List.of());
			for (var visit : recorded) visits.add(cityVisitKey(visit.getOperationId()));
			// Older internal callers may have recorded visits without operation IDs.
			for (int index = recorded.size(); index < entry.getVisitCount(); index++) {
				visits.add("legacy:" + stampKey + ":" + index);
			}
		}
		List<VisitedCity> cities = cityNames.entrySet().stream()
				.map(city -> new VisitedCity(city.getKey(), city.getValue(), cityVisits.get(city.getKey()).size())).toList();
		var busCounts = journeys.stream().map(PassportJourney::getRouteNumber)
				.filter(number -> number != null && !number.isBlank())
				.collect(Collectors.groupingBy(number -> number, java.util.TreeMap::new, Collectors.counting()));
		List<RiddenBus> buses = busCounts.entrySet().stream()
				.map(bus -> new RiddenBus(bus.getKey(), bus.getValue())).toList();
		return new TravelStats(totalDistanceKilometers, buses.size(), cities, List.copyOf(busCounts.keySet()), buses);
	}

	private String cityVisitKey(String operationId) {
		// finishRide uses <journeyKey>:stop:<index>:<locationId> and :town:<index>.
		// The same city encountered at multiple stops (or saved again as a town) is one visit.
		var match = RIDE_OPERATION.matcher(operationId);
		return match.matches() ? "ride:" + match.group(1) : "operation:" + operationId;
	}

	private Map<String, Object> findLocation(long locationId) {
		return emberClient.findLocationById(locationId).orElse(null);
	}

	private Double findRouteDistanceKilometers(List<Long> locationIds) {
		if (locationIds.size() == 1) return 0.0;
		var coordinates = new java.util.HashMap<Long, Coordinates>();
		var missing = new java.util.ArrayList<Long>();
		// Resolve each location once, but preserve repeated visits when summing legs.
		for (long id : locationIds.stream().distinct().toList()) {
			var stamp = stampRepository.findByLocationId(id);
			if (stamp.isPresent() && stamp.get().getLatitude() != null && stamp.get().getLongitude() != null) {
				coordinates.put(id, new Coordinates(stamp.get().getLatitude(), stamp.get().getLongitude()));
			} else missing.add(id);
		}
		try {
			for (int index = 0; index < missing.size(); index += 50) {
				var batch = missing.subList(index, Math.min(index + 50, missing.size()));
				var locations = emberClient.findLocationsByIds(batch);
				for (long id : batch) {
					var location = locations.get(id);
					if (location == null) return null;
					Double latitude = getCoordinate(location, "lat");
					Double longitude = getCoordinate(location, "lon");
					if (latitude == null || longitude == null) return null;
					coordinates.put(id, new Coordinates(latitude, longitude));
				}
			}
		} catch (EmberApiException exception) {
			logger.warn("Could not resolve journey stop coordinates from Ember: {}", exception.getMessage());
			return null;
		}
		double total = 0;
		for (int index = 1; index < locationIds.size(); index++) {
			var from = coordinates.get(locationIds.get(index - 1));
			var to = coordinates.get(locationIds.get(index));
			total += GeoDistance.kilometers(from.latitude(), from.longitude(), to.latitude(), to.longitude());
		}
		return total;
	}

	private record Coordinates(double latitude, double longitude) {}

	private Double findDistanceKilometers(long originLocationId, long destinationLocationId) {
		var originStamp = stampRepository.findByLocationId(originLocationId);
		var destinationStamp = stampRepository.findByLocationId(destinationLocationId);
		if (originStamp.isPresent() && destinationStamp.isPresent()
				&& originStamp.get().getLatitude() != null && originStamp.get().getLongitude() != null
				&& destinationStamp.get().getLatitude() != null && destinationStamp.get().getLongitude() != null) {
			return GeoDistance.kilometers(originStamp.get().getLatitude(), originStamp.get().getLongitude(),
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

		return GeoDistance.kilometers(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
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

	public record VisitedCity(String key, String name, long visitCount) {}
	public record RiddenBus(String routeNumber, long rideCount) {}

	public record TravelStats(Double totalDistanceKilometers, long busNumbersRidden, List<VisitedCity> cities,
			List<String> busNumbers, List<RiddenBus> buses) {
		public long townsVisited() {
			return cities.size();
		}
	}
}
