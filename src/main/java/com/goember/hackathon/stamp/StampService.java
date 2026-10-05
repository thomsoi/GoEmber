package com.goember.hackathon.stamp;

import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.ember.EmberApiException;
import jakarta.persistence.EntityNotFoundException;

@Service
public class StampService {
    private final StampRepository stampRepository;
    private final EmberClient emberClient;
    private final TransactionTemplate catalogueTransaction;
    private final Object catalogueLock = new Object();

    public StampService(StampRepository stampRepository, EmberClient emberClient,
            PlatformTransactionManager transactionManager) {
        this.stampRepository = stampRepository;
        this.emberClient = emberClient;
        catalogueTransaction = new TransactionTemplate(transactionManager);
        catalogueTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Stamp getOrCreateForLocation(long locationId) {
        return getOrCreateForLocation(locationId, null);
    }

    public Stamp getOrCreateForLocation(long locationId, String ignoredClientName) {
        if (locationId <= 0) throw new IllegalArgumentException("Location ID must be positive");
        var existing = stampRepository.findByLocationId(locationId);
        if (existing.isPresent()) return existing.get();
        Map<String, Object> location = emberClient.findLocationById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Ember location not found: " + locationId));
        if (!(location.get("name") instanceof String name) || name.isBlank()) {
            throw new EmberApiException("Ember returned a location without a name", null, false);
        }
        return saveCatalogueStamp(toLocationStamp(locationId, location));
    }

    public Map<Long, Stamp> getOrCreateForLocations(List<Long> locationIds) {
        var stamps = new HashMap<Long, Stamp>();
        var missing = new java.util.ArrayList<Long>();
        for (long locationId : locationIds.stream().distinct().toList()) {
            if (locationId <= 0) throw new IllegalArgumentException("Location ID must be positive");
            var existing = stampRepository.findByLocationId(locationId);
            if (existing.isPresent()) stamps.put(locationId, existing.get());
            else missing.add(locationId);
        }
        if (!missing.isEmpty()) {
            var locations = emberClient.findLocationsByIds(missing);
            for (long locationId : missing) {
                var location = locations.get(locationId);
                if (location == null) throw new EntityNotFoundException("Ember location not found: " + locationId);
                if (!(location.get("name") instanceof String name) || name.isBlank()) {
                    throw new EmberApiException("Ember returned a location without a name", null, false);
                }
            }
            for (long locationId : missing) {
                stamps.put(locationId, saveCatalogueStamp(toLocationStamp(locationId, locations.get(locationId))));
            }
        }
        return stamps;
    }

    public Stamp getOrCreateForTown(String townName) {
        if (townName == null || townName.isBlank() || townName.length() > 100) {
            throw new IllegalArgumentException("Town must contain between 1 and 100 characters");
        }
        String normalized = normalize(townName);
        String key = "town:" + normalized;
        var existing = stampRepository.findByStampKey(key);
        if (existing.isPresent()) return existing.get();
        var locations = emberClient.searchLocations(townName.trim(), 50, "STOP_POINT");
        String canonicalName = locations.stream()
                .map(location -> location.get("region_name"))
                .filter(String.class::isInstance).map(String.class::cast)
                .filter(name -> normalize(name).equals(normalized)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Ember town not found: " + townName));
        Stamp townStamp = new Stamp(key, canonicalName.trim());
        townStamp.setRegionName(canonicalName.trim());
        return saveCatalogueStamp(townStamp);
    }

    private Stamp toLocationStamp(long locationId, Map<String, Object> location) {
        // Keep source metadata so future city mappings can also repair existing visits.
        String region = location.get("region_name") instanceof String name && !name.isBlank() ? name.trim() : null;
        return new Stamp(locationId, (String) location.get("name"), region,
                coordinate(location.get("lat"), 90), coordinate(location.get("lon"), 180));
    }

    private Double coordinate(Object value, double max) {
        if (!(value instanceof Number number)) return null;
        double coordinate = number.doubleValue();
        return Double.isFinite(coordinate) && Math.abs(coordinate) <= max ? coordinate : null;
    }

    private Stamp saveCatalogueStamp(Stamp candidate) {
        // This application has one process and an in-memory database. Keep the lock
        // until commit so concurrent passports cannot create the same catalogue row.
        synchronized (catalogueLock) {
            return catalogueTransaction.execute(status -> stampRepository.findByStampKey(candidate.getStampKey())
                    .orElseGet(() -> stampRepository.saveAndFlush(candidate)));
        }
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
