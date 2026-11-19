package com.vaccination.exception;

/**
 * Exception thrown when appointment booking or cancellation workflows fail
 * (e.g., slot fully booked, duplicate active booking, past slot cancellation).
 */
public class BookingException extends AppException {

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
