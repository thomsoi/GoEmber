package com.goember.hackathon.geo;

/** Great-circle distance used by stop discovery and estimated journey statistics. */
public final class GeoDistance {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistance() {
    }

    public static double kilometers(double latitude1, double longitude1,
            double latitude2, double longitude2) {
        double latitudeDifference = Math.toRadians(latitude2 - latitude1);
        double longitudeDifference = Math.toRadians(longitude2 - longitude1);
        double haversine = Math.pow(Math.sin(latitudeDifference / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
        // Floating-point rounding at antipodal points must not produce NaN.
        haversine = Math.max(0, Math.min(1, haversine));
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }
}
