package com.goember.hackathon;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.goember.hackathon.passport.CityNames;

class CityNamesTest {
    @Test
    void airportsResolveToSettlementsAndUnknownFacilitiesAreNotCities() {
        assertEquals("Edinburgh", CityNames.canonical("  EDINBURGH   AIRPORT "));
        assertEquals("Edinburgh", CityNames.canonical("Edinburgh Airport Hotels"));
        assertEquals("Aberdeen", CityNames.canonical("Aberdeen Airport"));
        assertEquals("Paisley", CityNames.canonical("Glasgow Airport"));
        for (String facility : new String[] { "Buchanan Bus Station", "Unknown Airport",
                "Halbeath Park & Ride", "Broxden Park and Ride", "Harthill Services", "Airport Terminal" }) {
            assertNull(CityNames.canonical(facility), facility);
        }
        assertNull(CityNames.canonical(null));
        assertNull(CityNames.canonical(" "));
        assertEquals("Ratho Station", CityNames.canonical("Ratho Station"));
        assertEquals("Bridge of Earn", CityNames.canonical("Bridge of Earn"));
    }
}
