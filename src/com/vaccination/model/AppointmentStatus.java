package com.vaccination.model;

/**
 * Represents the lifecycle statuses of an appointment.
 */
public enum AppointmentStatus {
    BOOKED,
    CANCELLED,
    COMPLETED,
    MISSED;

    public static AppointmentStatus fromString(String statusStr) {
        if (statusStr == null) {
            return BOOKED;
        }
        try {
            return AppointmentStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOOKED;
        }
    }
}
