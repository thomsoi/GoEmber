package com.goember.hackathon.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goember.hackathon.ember.EmberClient;
import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.PassportStampRepository;
import com.goember.hackathon.stamp.Stamp;
import com.goember.hackathon.stamp.StampService;
import com.goember.hackathon.user.User;
import com.goember.hackathon.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PassportServiceTest {

    @Mock
    private PassportRepository passportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StampService stampService;

    @Mock
    private PassportStampRepository passportStampRepository;

    @Mock
    private PassportJourneyRepository passportJourneyRepository;

    @Mock
    private EmberClient emberClient;

    private PassportService passportService;

    @BeforeEach
    void setUp() {
        passportService = new PassportService(
            passportRepository,
            userRepository,
            stampService,
            passportStampRepository,
            passportJourneyRepository,
            emberClient);
    }

    @Test
    void getOrCreatePassport_returnsExistingPassport() {
        User user = new User("Alice");
        Passport expected = new Passport(user);
        when(passportRepository.findByUser_Id(1L)).thenReturn(Optional.of(expected));

        Passport passport = passportService.getOrCreatePassport(1L);

        assertNotNull(passport);
        assertEquals(user, passport.getUser());
    }

    @Test
    void recordLocationVisit_createsStampOnFirstVisit() {
        User user = new User("Alice");
        Passport passport = new Passport(user);
        Stamp stamp = new Stamp(42L, "Inverkeithing");
        when(passportRepository.findByUser_Id(1L)).thenReturn(Optional.of(passport));
        when(stampService.getOrCreateForLocation(42L)).thenReturn(stamp);

        PassportStamp result = passportService.recordLocationVisit(1L, 42L);

        assertNotNull(result);
        assertEquals(1, result.getVisitCount());
        assertEquals("Inverkeithing", result.getStamp().getName());
    }

    @Test
    void recordLocationVisit_incrementsExistingStamp() {
        User user = new User("Alice");
        Passport passport = new Passport(user);
        Stamp stamp = new Stamp(42L, "Inverkeithing");
        PassportStamp existing = passport.addStamp(stamp);
        when(passportRepository.findByUser_Id(1L)).thenReturn(Optional.of(passport));
        when(stampService.getOrCreateForLocation(42L)).thenReturn(stamp);

        PassportStamp result = passportService.recordLocationVisit(1L, 42L);

        assertEquals(2, result.getVisitCount());
    }

    @Test
    void recordTownVisit_reusesTownStampAndIncrementsVisitCount() {
        User user = new User("Alice");
        Passport passport = new Passport(user);
        Stamp townStamp = new Stamp("town:aberdeen", "Aberdeen");
        when(passportRepository.findByUser_Id(1L)).thenReturn(Optional.of(passport));
        when(stampService.getOrCreateForTown("Aberdeen")).thenReturn(townStamp);

        PassportStamp firstVisit = passportService.recordTownVisit(1L, "Aberdeen");
        PassportStamp secondVisit = passportService.recordTownVisit(1L, "Aberdeen");

        assertEquals(firstVisit, secondVisit);
        assertEquals(2, secondVisit.getVisitCount());
        assertEquals("town:aberdeen", secondVisit.getStamp().getStampKey());
        assertEquals(1, passport.getStamps().size());
    }

    @Test
    void percentOfUsersWithStamp_returnsPercentOfAllUsers() {
        when(userRepository.count()).thenReturn(20L);
        when(passportStampRepository.countByStamp_LocationId(42L)).thenReturn(5L);

		double percentage = passportService.percentOfUsersWithStamp(42L);

		assertEquals(25.0, percentage);
    }

    @Test
    void recordCompletedJourney_savesDistanceAndUniqueTowns() {
        Passport passport = new Passport(new User("Alice"));
        when(passportRepository.findByUser_Id(1L)).thenReturn(Optional.of(passport));
        when(passportJourneyRepository.findByPassport_PassportIdAndJourneyKey(null, "journey-1"))
                .thenReturn(Optional.empty());
        when(emberClient.findLocationById(10L)).thenReturn(Optional.of(Map.of("lat", 0.0, "lon", 0.0)));
        when(emberClient.findLocationById(20L)).thenReturn(Optional.of(Map.of("lat", 0.0, "lon", 1.0)));

        passportService.recordCompletedJourney(
                1L,
                "journey-1",
                10L,
                20L,
                "Aberdeen",
                "Dundee",
                List.of("Aberdeen", "Dundee", "Dundee"));

        ArgumentCaptor<PassportJourney> journeyCaptor = ArgumentCaptor.forClass(PassportJourney.class);
        verify(passportJourneyRepository).save(journeyCaptor.capture());
        assertEquals(111.2, journeyCaptor.getValue().getDistanceKilometers(), 0.1);
        assertEquals(Set.of("aberdeen", "dundee"), journeyCaptor.getValue().getTowns());
    }

    @Test
    void recordCompletedJourney_savesTownsWhenEmberLocationsAreUnavailable() {
        Passport passport = new Passport(new User("Alice"));
        when(passportRepository.findByUser_Id(1L)).thenReturn(Optional.of(passport));
        when(passportJourneyRepository.findByPassport_PassportIdAndJourneyKey(null, "journey-2"))
                .thenReturn(Optional.empty());
        when(emberClient.findLocationById(10L)).thenReturn(Optional.empty());
        when(emberClient.findLocationById(20L)).thenReturn(Optional.empty());
        when(emberClient.searchLocations("Aberdeen", 50, "STOP_POINT")).thenReturn(List.of());
        when(emberClient.searchLocations("Dundee", 50, "STOP_POINT")).thenReturn(List.of());

        passportService.recordCompletedJourney(
                1L,
                "journey-2",
                10L,
                20L,
                "Aberdeen",
                "Dundee",
                List.of("Aberdeen", "Dundee"));

        ArgumentCaptor<PassportJourney> journeyCaptor = ArgumentCaptor.forClass(PassportJourney.class);
        verify(passportJourneyRepository).save(journeyCaptor.capture());
        assertNull(journeyCaptor.getValue().getDistanceKilometers());
        assertEquals(Set.of("aberdeen", "dundee"), journeyCaptor.getValue().getTowns());
    }

    @Test
    void getTravelStats_countsUniqueTownsAndRecordedRoutes() {
        Passport passport = new Passport(new User("Alice"));
        List<PassportJourney> journeys = List.of(
                new PassportJourney(passport, "journey-1", 100.0, Set.of("aberdeen", "dundee")),
                new PassportJourney(passport, "journey-2", 25.0, Set.of("dundee", "perth")));
        when(passportJourneyRepository.findAllByPassport_PassportId(1L)).thenReturn(journeys);

        PassportService.TravelStats stats = passportService.getTravelStats(1L);

        assertEquals(125.0, stats.totalDistanceKilometers());
        assertEquals(2, stats.routesTravelled());
        assertEquals(3, stats.townsVisited());
    }
}
