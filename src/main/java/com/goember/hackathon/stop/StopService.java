package com.goember.hackathon.stop;

import java.util.List;

import org.springframework.stereotype.Service;

import com.goember.hackathon.geo.GeoDistance;
import com.goember.hackathon.ember.EmberService;

@Service
public class StopService {

    private static final double DISCOVERY_RADIUS_KM = 0.5;

    private final EmberService emberService;

    public StopService(EmberService emberService) {
        this.emberService = emberService;
    }

    public List<Stop> getStops() {
        return emberService.getStops();
    }

    public Stop findNearestStop(
            double latitude,
            double longitude
    ) {
        return findNearestStop(latitude, longitude, null);
    }

    public Stop findNearestStop(
            double latitude,
            double longitude,
            List<Long> allowedStopIds
    ) {
        List<Stop> stops = getStops();

        if (allowedStopIds != null && !allowedStopIds.isEmpty()) {
            stops = stops.stream()
                    .filter(stop -> stop.getEmberLocationId() != null && allowedStopIds.contains(stop.getEmberLocationId()))
                    .toList();
        }

        Stop nearestStop = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Stop stop : stops) {
            double distance = GeoDistance.kilometers(
                    latitude,
                    longitude,
                    stop.getLatitude(),
                    stop.getLongitude()
            );

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestStop = stop;
            }
        }

        if (nearestStop == null || nearestDistance > DISCOVERY_RADIUS_KM) {
            return null;
        }

        return nearestStop;
    }

    public List<Stop> findStopsWithinRadius(
        double latitude,
        double longitude,
        double radiusKm
    )
    {
        return getStops().stream()
                .filter(stop -> GeoDistance.kilometers(
                        latitude,
                        longitude,
                        stop.getLatitude(),
                        stop.getLongitude()
                ) <= radiusKm)
                .toList();
    }

}
