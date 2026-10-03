package com.goember.hackathon.stamp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StampTierTest {

    @Test
    void assignsTiersAtVisitThresholds() {
        assertEquals(StampTier.UNTIERED, StampTier.forVisitCount(1));
        assertEquals(StampTier.UNTIERED, StampTier.forVisitCount(9));
        assertEquals(StampTier.BRONZE, StampTier.forVisitCount(10));
        assertEquals(StampTier.BRONZE, StampTier.forVisitCount(24));
        assertEquals(StampTier.SILVER, StampTier.forVisitCount(25));
        assertEquals(StampTier.SILVER, StampTier.forVisitCount(49));
        assertEquals(StampTier.GOLD, StampTier.forVisitCount(50));
    }
}