package com.goember.hackathon.stop;

import java.util.List;

import org.springframework.stereotype.Service;

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
        List<Stop> stops = getStops();

        Stop nearestStop = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Stop stop : stops) {

            double distance = distanceInKm(
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
                .filter(stop -> distanceInKm(
                        latitude,
                        longitude,
                        stop.getLatitude(),
                        stop.getLongitude()
                ) <= radiusKm)
                .toList();
    }

    private double distanceInKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        double earthRadius = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return earthRadius * c;
    }
}