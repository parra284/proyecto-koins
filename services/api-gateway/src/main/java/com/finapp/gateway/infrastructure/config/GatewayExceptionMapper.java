package com.finapp.gateway.infrastructure.config;

import com.finapp.gateway.domain.exception.GatewayDomainException;
import com.finapp.gateway.domain.exception.InvalidSessionTokenException;
import com.finapp.gateway.domain.exception.RateLimitExceededException;
import com.finapp.gateway.domain.exception.RouteNotFoundException;
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
 * Global exception handler — intercepts all exceptions thrown during
 * gateway processing and transforms them into secure, standardized
 * HTTP error responses (Fail-Safe pattern).
 *
 * <p><strong>Security principle:</strong> Never leak internal stack traces,
 * class names, or implementation details to external clients. All error
 * responses follow a consistent JSON structure.</p>
 */
@RestControllerAdvice
public class GatewayExceptionMapper {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionMapper.class);

    /**
     * Handles invalid, expired, or tampered session tokens.
     * Returns HTTP 401 Unauthorized.
     */
    @ExceptionHandler(InvalidSessionTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidSessionTokenException ex) {
        log.warn("Session token validation failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed",
                "The provided session token is invalid or has expired.");
    }

    /**
     * Handles rate-limit violations.
     * Returns HTTP 429 Too Many Requests.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded for client: {}", ex.clientId());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded",
                "You have exceeded the allowed request rate. Please retry later.");
    }

    /**
     * Handles unmatched routes.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRouteNotFound(RouteNotFoundException ex) {
        log.info("No route found for path: {}", ex.requestPath());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Route not found",
                "The requested resource path does not exist.");
    }

    /**
     * Handles missing or malformed Authorization header.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad request",
                "The request is malformed or missing required parameters.");
    }

    /**
     * Catches any other domain exception not explicitly mapped above.
     * Returns HTTP 500 Internal Server Error with a safe message.
     */
    @ExceptionHandler(GatewayDomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(GatewayDomainException ex) {
        log.error("Unhandled domain exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred. Please contact support.");
    }

    /**
     * Ultimate fallback — catches absolutely everything else.
     * Returns HTTP 500 with a safe, opaque message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected exception in gateway pipeline", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred. Please contact support.");
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Builds a standardized error response body.
     * <strong>Never</strong> includes stack traces or internal details.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message) {

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
