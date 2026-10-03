package com.goember.hackathon.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goember.hackathon.stamp.PassportStamp;
import com.goember.hackathon.stamp.Stamp;
import com.goember.hackathon.user.User;

@ExtendWith(MockitoExtension.class)
class PassportControllerTest {

    @Mock
    private PassportService passportService;

    private PassportController passportController;

    @BeforeEach
    void setUp() {
        passportController = new PassportController(passportService);
    }

    @Test
    void getPassport_returnsPassportResponse() {
        User user = new User("Alice");
        Passport passport = new Passport(user);
        Stamp stamp = new Stamp(42L, "Inverkeithing");
        PassportStamp passportStamp = passport.addStamp(stamp);

        when(passportService.getOrCreatePassport(1L)).thenReturn(passport);
        when(passportService.getTravelStats(null))
                .thenReturn(new PassportService.TravelStats(125.0, 2, 3));

        PassportController.PassportResponse response = passportController.getPassport(1L);

        assertNotNull(response);
        assertEquals(1, response.stamps().size());
        assertEquals("Inverkeithing", response.stamps().get(0).stampName());
        assertEquals(125.0, response.totalDistanceTravelled());
        assertEquals(2, response.routesTravelled());
        assertEquals(3, response.townsVisited());
    }

    @Test
    void recordLocationVisit_returnsVisitResponse() {
        User user = new User("Alice");
        Passport passport = new Passport(user);
        Stamp stamp = new Stamp(42L, "Inverkeithing");
        PassportStamp passportStamp = passport.addStamp(stamp);
        passportStamp.recordVisit();

        when(passportService.recordLocationVisit(1L, 42L, "Inverkeithing")).thenReturn(passportStamp);

        PassportController.StampVisitResponse response = passportController.recordLocationVisit(
            1L,
            42L,
            new PassportController.LocationVisitRequest("Inverkeithing"));

        assertNotNull(response);
        assertEquals(42L, response.locationId());
        assertEquals("Inverkeithing", response.stampName());
        assertEquals(2, response.visitCount());
        assertEquals(Boolean.TRUE, response.alreadyVisited());
    }
}
