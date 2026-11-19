package com.vaccination.model;

/**
 * Represents system user permission levels.
 */
public enum Role {
    USER,
    ADMIN;

    public static Role fromString(String roleStr) {
        if (roleStr == null) {
            return USER;
        }
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
