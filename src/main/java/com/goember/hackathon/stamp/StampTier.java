package com.goember.hackathon.stamp;

public enum StampTier {
    UNTIERED("untiered"),
    BRONZE("bronze"),
    SILVER("silver"),
    GOLD("gold");

    private final String value;

    StampTier(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StampTier forVisitCount(int visitCount) {
        if (visitCount >= 50) {
            return GOLD;
        }
        if (visitCount >= 25) {
            return SILVER;
        }
        if (visitCount >= 10) {
            return BRONZE;
        }
        return UNTIERED;
    }
}