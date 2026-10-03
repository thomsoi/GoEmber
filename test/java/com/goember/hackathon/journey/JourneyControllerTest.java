package com.goember.hackathon.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JourneyControllerTest {

    @Mock
    private JourneyService journeyService;

    private JourneyController journeyController;

    @BeforeEach
    void setUp() {
        journeyController = new JourneyController(journeyService);
    }

    @Test
    void createJourney_delegatesToService() {
        LocalDateTime departure = LocalDateTime.of(2026, 10, 3, 9, 0);
        LocalDateTime arrival = LocalDateTime.of(2026, 10, 3, 10, 30);
        Journey expected = new Journey(1L, 44L, 101L, 202L, "Dundee", "Edinburgh", departure, arrival);

        when(journeyService.createJourney(1L, 44L, 101L, 202L, "Dundee", "Edinburgh", departure, arrival))
                .thenReturn(expected);

        Journey created = journeyController.createJourney(new JourneyController.CreateJourneyRequest(
                1L,
                44L,
                101L,
                202L,
                "Dundee",
                "Edinburgh",
                departure,
                arrival));

        assertNotNull(created);
        assertEquals("Dundee", created.getOriginName());
        assertEquals("Edinburgh", created.getDestinationName());
    }

    @Test
    void getJourney_returnsJourney() {
        Journey expected = new Journey(2L, 55L, 11L, 12L, "A", "B", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        when(journeyService.getJourney(99L)).thenReturn(expected);

        Journey actual = journeyController.getJourney(99L);

        assertNotNull(actual);
        assertEquals(55L, actual.getEmberTripId());
    }

    @Test
    void startJourney_delegatesToService() {
        Journey started = new Journey(4L, 12L, 1L, 2L, "X", "Y", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        started.setActive(true);
        when(journeyService.startJourney(5L)).thenReturn(started);

        Journey actual = journeyController.startJourney(5L);

        assertEquals(true, actual.isActive());
    }

    @Test
    void completeJourney_delegatesToService() {
        Journey completed = new Journey(4L, 12L, 1L, 2L, "X", "Y", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        completed.setActive(false);
        when(journeyService.completeJourney(6L)).thenReturn(completed);

        Journey actual = journeyController.completeJourney(6L);

        assertEquals(false, actual.isActive());
    }

    @Test
    void getActiveJourney_returnsOptionalJourney() {
        Journey active = new Journey(4L, 12L, 1L, 2L, "A", "B", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        when(journeyService.getActiveJourney(4L)).thenReturn(Optional.of(active));

        Optional<Journey> actual = journeyController.getActiveJourney(4L);

        assertTrue(actual.isPresent());
        assertEquals(12L, actual.get().getEmberTripId());
    }
}
