package com.goember.hackathon.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goember.hackathon.ember.EmberService;

@ExtendWith(MockitoExtension.class)
class StopServiceTest {

    @Mock
    private EmberService emberService;

    @Test
    void getStops_returnsStopsFromEmberService() {
        StopService stopService = new StopService(emberService);
        List<Stop> stops = List.of(
                new Stop(1L, "Stop A", "STOP_POINT", 10L, 56.0, -3.0),
                new Stop(2L, "Stop B", "STOP_POINT", 11L, 57.0, -3.0));
        when(emberService.getStops()).thenReturn(stops);

        List<Stop> actual = stopService.getStops();

        assertEquals(2, actual.size());
        assertEquals("Stop A", actual.get(0).getName());
    }

    @Test
    void findNearestStop_returnsClosestWithinRadius() {
        StopService stopService = new StopService(emberService);
        List<Stop> stops = List.of(
                new Stop(1L, "Far Stop", "STOP_POINT", 10L, 56.3, -3.2),
                new Stop(2L, "Near Stop", "STOP_POINT", 11L, 56.0, -3.0));
        when(emberService.getStops()).thenReturn(stops);

        Stop nearest = stopService.findNearestStop(56.0, -3.0);

        assertNotNull(nearest);
        assertEquals("Near Stop", nearest.getName());
    }

    @Test
    void findNearestStop_returnsNullWhenNoStopWithinRadius() {
        StopService stopService = new StopService(emberService);
        when(emberService.getStops()).thenReturn(List.of(
                new Stop(1L, "Distant", "STOP_POINT", 10L, 60.0, -4.0)));

        Stop nearest = stopService.findNearestStop(56.0, -3.0);

        assertNull(nearest);
    }

    @Test
    void findStopsWithinRadius_filtersOnlyWithinRadius() {
        StopService stopService = new StopService(emberService);
        when(emberService.getStops()).thenReturn(List.of(
                new Stop(1L, "Near", "STOP_POINT", 10L, 56.0, -3.0),
                new Stop(2L, "Far", "STOP_POINT", 11L, 58.0, -3.0)));

        List<Stop> actual = stopService.findStopsWithinRadius(56.0, -3.0, 50.0);

        assertEquals(1, actual.size());
        assertEquals("Near", actual.get(0).getName());
    }
}
