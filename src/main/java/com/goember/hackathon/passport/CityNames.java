package com.goember.hackathon.passport;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Converts Ember region labels to passport settlements, never from stop names. */
public final class CityNames {
    private CityNames() {}

    // Explicit mappings: an airport's marketed city is not always its settlement.
    private static final Map<String, String> ALIASES = Map.of(
            "edinburgh airport", "Edinburgh",
            "edinburgh airport hotels", "Edinburgh",
            "aberdeen airport", "Aberdeen",
            "glasgow airport", "Paisley");
    private static final Pattern FACILITY = Pattern.compile(
            "(?i)\\b(airport|terminal|hotels?|services|bus station|railway station|train station|"
            + "park (and|&) ride|p&r|car park|retail park|business park|shopping centre|hospital|university)\\b");

    public static String canonical(String region) {
        if (region == null || region.isBlank()) return null;
        String name = region.trim().replaceAll("\\s+", " ");
        String alias = ALIASES.get(name.toLowerCase(Locale.ROOT));
        if (alias != null) return alias;
        // Do not guess a city by removing a facility suffix (e.g. Buchanan Bus Station).
        if (FACILITY.matcher(name).find()) return null;
        return name;
    }
}
