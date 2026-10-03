package com.goember.hackathon.ember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmberControllerTest {

    @Mock
    private EmberService emberService;

    private EmberController emberController;

    @BeforeEach
    void setUp() {
        emberController = new EmberController(emberService);
    }

    @Test
    void getStops_returnsStopPayloads() {
        Map<String, Object> stop = Map.of(
                "id", 101L,
                "name", "Inverkeithing",
                "type", "STOP_POINT",
                "area_id", 10L,
                "lat", 56.03,
                "lon", -3.40);
        when(emberService.getStopPoints()).thenReturn(List.of(stop));

        List<Map<String, Object>> stops = emberController.getStops();

        assertEquals(1, stops.size());
        assertEquals("Inverkeithing", stops.get(0).get("name"));
    }

    @Test
    void getNearbyStops_filtersByRadius() {
        Map<String, Object> nearStop = Map.of(
                "id", 101L,
                "name", "Inverkeithing",
                "type", "STOP_POINT",
                "area_id", 10L,
                "lat", 56.03,
                "lon", -3.40);
        Map<String, Object> farStop = Map.of(
                "id", 102L,
                "name", "Dundee",
                "type", "STOP_POINT",
                "area_id", 11L,
                "lat", 56.46,
                "lon", -2.97);
        when(emberService.getStopPoints()).thenReturn(List.of(nearStop, farStop));

        List<Map<String, Object>> nearbyStops = emberController.getNearbyStops(56.03, -3.40, 5.0);

        assertEquals(1, nearbyStops.size());
        assertEquals("Inverkeithing", nearbyStops.get(0).get("name"));
    }

    @Test
    void getTrip_returnsTripPayload() {
        Map<String, Object> trip = Map.of("tripId", 44L, "route", List.of("stop-1", "stop-2"));
        when(emberService.getTrip(44L)).thenReturn(trip);

        Map<String, Object> actual = emberController.getTrip(44L);

        assertNotNull(actual);
        assertEquals(44L, actual.get("tripId"));
    }
}
