package com.goember.hackathon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.ember.EmberService;
import com.goember.hackathon.geo.GeoDistance;
import com.goember.hackathon.stop.StopService;

class StopDiscoveryTest {
    @Test
    void nearbyEndpointsAgreeAndNearestSearchHonorsAllowedStops() {
        var client = mock(EmberClient.class);
        Map<String, Object> near = Map.of("id", 10L, "name", "Near", "lat", 56.0, "lon", -3.0);
        Map<String, Object> far = Map.of("id", 20L, "name", "Far", "lat", 57.0, "lon", -3.0);
        when(client.searchLocations("", 50, "STOP_POINT")).thenReturn(List.of(near, far));
        var ember = new EmberService(client);
        var stops = new StopService(ember);

        assertEquals(List.of(near), ember.getNearbyStopPoints(56, -3, 0.5));
        assertEquals(List.of(10L), stops.findStopsWithinRadius(56, -3, 0.5).stream()
                .map(stop -> stop.getEmberLocationId()).toList());
        assertEquals(10L, stops.findNearestStop(56, -3).getEmberLocationId());
        assertNull(stops.findNearestStop(56, -3, List.of(20L)));
    }

    @Test
    void distanceHandlesIdenticalAndAntipodalPoints() {
        assertEquals(0, GeoDistance.kilometers(56, -3, 56, -3));
        assertEquals(Math.PI * 6371, GeoDistance.kilometers(0, 0, 0, 180), 0.001);
        assertEquals(111.195, GeoDistance.kilometers(0, 0, 1, 0), 0.001);
    }
}
