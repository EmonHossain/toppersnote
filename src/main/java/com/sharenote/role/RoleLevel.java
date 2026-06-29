package com.sharenote.role;

import java.util.Optional;

public enum RoleLevel {
    USER_DEFAULT,
    USER,
    ADMIN,
    SUPER_ADMIN;

    /**
     * Finds a RoleLevel matching the given string name safely (Case-Insensitive).
     * 
     * @param name The string name of the enum
     * @return An Optional containing the enum constant, or empty if not found.
     */
    public static Optional<RoleLevel> fromString(String name) {
        if (name == null) {
            return Optional.empty();
        }

        try {
            // Converts input to uppercase and trims whitespaces for resilience
            return Optional.of(RoleLevel.valueOf(name.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}