package com.finapp.auth.infrastructure.config;

import com.finapp.auth.domain.exception.AccountLockedException;
import com.finapp.auth.domain.exception.AuthDomainException;
import com.finapp.auth.domain.exception.DuplicateEmailException;
import com.finapp.auth.domain.exception.InvalidCredentialsException;
import com.finapp.auth.domain.exception.InvalidEmailException;
import com.finapp.auth.domain.exception.WeakPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler — intercepts all domain and application
 * exceptions and transforms them into secure, standardized HTTP error
 * responses (Fail-Safe pattern).
 *
 * <p><strong>Security principle:</strong> Never leak internal stack traces,
 * class names, implementation details, or timing information to external
 * clients. All error responses follow a consistent JSON structure.</p>
 */
@RestControllerAdvice
public class AuthExceptionMapper {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionMapper.class);

    /**
     * Invalid email format.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEmail(InvalidEmailException ex) {
        log.warn("Invalid email: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid email",
                ex.getMessage());
    }

    /**
     * Weak password (complexity violations).
     * Returns HTTP 400 Bad Request with violation details.
     */
    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWeakPassword(WeakPasswordException ex) {
        log.warn("Weak password: {} violations", ex.violations().size());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password does not meet requirements",
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", "Password does not meet requirements",
                        "violations", ex.violations()
                ));
    }

    /**
     * Duplicate email registration.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn("Duplicate email registration attempt: {}", ex.email());
        return buildErrorResponse(HttpStatus.CONFLICT, "Registration failed",
                "An account with this email already exists.");
    }

    /**
     * Invalid credentials (wrong email or password).
     * Returns HTTP 401 Unauthorized with generic message (anti-enumeration).
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Failed authentication attempt");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed",
                "Invalid email or password.");
    }

    /**
     * Account locked due to brute-force.
     * Returns HTTP 423 Locked with generic message (no timing leak).
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException ex) {
        log.warn("Login attempt on locked account: {}", ex.email());
        return buildErrorResponse(HttpStatus.LOCKED, "Account locked",
                "Account is temporarily locked. Please try again later.");
    }

    /**
     * Malformed request parameters.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad request",
                "The request is malformed or missing required parameters.");
    }

    /**
     * Catch-all for unhandled domain exceptions.
     * Returns HTTP 500 with safe message.
     */
    @ExceptionHandler(AuthDomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(AuthDomainException ex) {
        log.error("Unhandled domain exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred.");
    }

    /**
     * Ultimate fallback.
     * Returns HTTP 500 with opaque message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected exception in auth service", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred.");
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message) {
        return buildErrorResponse(status, error, Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message
        ));
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, Map<String, Object> body) {
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
