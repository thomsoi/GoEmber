package com.goember.hackathon.stamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.goember.hackathon.ember.EmberClient;

@ExtendWith(MockitoExtension.class)
class StampServiceTest {

    @Mock
    private StampRepository stampRepository;

    @Mock
    private EmberClient emberClient;

    private StampService stampService;

    @BeforeEach
    void setUp() {
        stampService = new StampService(stampRepository, emberClient);
    }

    @Test
    void getOrCreateForLocation_returnsExistingStamp() {
        Stamp existing = new Stamp(42L, "Inverkeithing");
        when(stampRepository.findByLocationId(42L)).thenReturn(Optional.of(existing));

        Stamp actual = stampService.getOrCreateForLocation(42L);

        assertNotNull(actual);
        assertEquals("Inverkeithing", actual.getName());
    }

    @Test
    void getOrCreateForLocation_createsFromEmberDataWhenMissing() {
        when(stampRepository.findByLocationId(42L)).thenReturn(Optional.empty());
        when(emberClient.findLocationById(42L)).thenReturn(Optional.of(Map.of("name", "Inverkeithing")));
        when(stampRepository.save(any(Stamp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Stamp actual = stampService.getOrCreateForLocation(42L);

        assertNotNull(actual);
        assertEquals("Inverkeithing", actual.getName());
    }
}
