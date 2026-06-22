package com.sharenote.user.enums;

public enum ProfileHealth {
    GREEN("healthy", 1),
    YELLOW("temporary_ban", 2),
    RED("permanent_ban", 3);

    private final String description;
    private final int code;

    ProfileHealth(String description, int code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public static ProfileHealth fromCode(int code) {
        for (ProfileHealth health : ProfileHealth.values()) {
            if (health.getCode() == code) {
                return health;
            }
        }
        throw new IllegalArgumentException("Invalid code for ProfileHealth: " + code);
    }

    public static ProfileHealth fromDescription(String description) {
        for (ProfileHealth health : ProfileHealth.values()) {
            if (health.getDescription().equalsIgnoreCase(description)) {
                return health;
            }
        }
        throw new IllegalArgumentException("Invalid description for ProfileHealth: " + description);
    }
}