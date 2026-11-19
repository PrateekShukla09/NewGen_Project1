package com.vaccination.exception;

/**
 * Base checked exception for all application-specific errors in the
 * Vaccination Drive Scheduler system.
 */
public class AppException extends Exception {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
