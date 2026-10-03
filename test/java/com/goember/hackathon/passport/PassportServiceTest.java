package com.goember.hackathon.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private PassportService passportService;

    @BeforeEach
    void setUp() {
        passportService = new PassportService(
            passportRepository,
            userRepository,
            stampService,
            passportStampRepository);
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
        assertEquals(2, result.getVisitCount());
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
    void percentOfUsersWithStamp_returnsPercentOfAllUsers() {
        when(userRepository.count()).thenReturn(20L);
        when(passportStampRepository.countByStamp_LocationId(42L)).thenReturn(5L);

		double percentage = passportService.percentOfUsersWithStamp(42L);

		assertEquals(25.0, percentage);
    }
}
