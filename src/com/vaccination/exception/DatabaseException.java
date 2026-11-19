package com.vaccination.exception;

/**
 * Exception thrown when a low-level JDBC or database operation fails.
 * Wraps SQLExceptions to avoid leaking implementation details into presentation layers.
 */
public class DatabaseException extends AppException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
