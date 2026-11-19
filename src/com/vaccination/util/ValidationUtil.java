package com.vaccination.util;

import com.vaccination.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

/**
 * Validates domain rules and user inputs across the application.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\-\\s()]{7,20}$"
    );

    public static void validateNotBlank(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be blank.");
        }
    }

    public static void validateName(String name, String fieldName) throws ValidationException {
        validateNotBlank(name, fieldName);
        if (name.trim().length() < 2 || name.trim().length() > 100) {
            throw new ValidationException(fieldName + " must be between 2 and 100 characters.");
        }
    }

    public static void validateEmail(String email) throws ValidationException {
        validateNotBlank(email, "Email");
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Invalid email format: '" + trimmed + "'. Example: user@domain.com");
        }
    }

    public static void validatePhone(String phone) throws ValidationException {
        validateNotBlank(phone, "Phone number");
        String trimmed = phone.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Invalid phone format: '" + trimmed + "'. Allowed: 7-20 digits, dashes, and parentheses.");
        }
    }

    public static void validatePasswordRegistration(String password, String confirmPassword) throws ValidationException {
        validateNotBlank(password, "Password");
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Password and confirmation password do not match.");
        }
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate) throws ValidationException {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Drive start date and end date must not be null.");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Drive start date (" + startDate + ") cannot be after end date (" + endDate + ").");
        }
    }

    public static void validateTimeRange(LocalTime startTime, LocalTime endTime) throws ValidationException {
        if (startTime == null || endTime == null) {
            throw new ValidationException("Slot start time and end time must not be null.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new ValidationException("Slot start time (" + startTime + ") must be before end time (" + endTime + ").");
        }
    }

    public static void validateCapacity(int capacity) throws ValidationException {
        if (capacity <= 0) {
            throw new ValidationException("Slot capacity must be greater than zero. Provided: " + capacity);
        }
    }
}
