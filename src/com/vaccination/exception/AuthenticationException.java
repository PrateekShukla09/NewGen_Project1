package com.vaccination.exception;

/**
 * Exception thrown when authentication or authorization operations fail
 * (e.g., incorrect password, disabled account, unauthorized role access).
 */
public class AuthenticationException extends AppException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
