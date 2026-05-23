package com.finapp.gateway.infrastructure.config;

import com.finapp.gateway.domain.exception.GatewayDomainException;
import com.finapp.gateway.domain.exception.InvalidSessionTokenException;
import com.finapp.gateway.domain.exception.RateLimitExceededException;
import com.finapp.gateway.domain.exception.RouteNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Global exception handler — intercepts all exceptions thrown during
 * gateway processing (including those from {@code WebFilter} pipelines)
 * and transforms them into secure, standardized HTTP error responses.
 *
 * <p>Implements {@link ErrorWebExceptionHandler} instead of
 * {@code @RestControllerAdvice} because the gateway's request pipeline
 * runs inside a {@code WebFilter}, not a controller. Spring's
 * {@code @ExceptionHandler} mechanism only captures exceptions thrown
 * from annotated controllers.</p>
 *
 * <p><strong>Security principle:</strong> Never leak internal stack traces,
 * class names, or implementation details to external clients. All error
 * responses follow a consistent JSON structure.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionMapper implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionMapper.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String error;
        String message;

        switch (ex) {
            case InvalidSessionTokenException iste -> {
                status = HttpStatus.UNAUTHORIZED;
                error = "Authentication failed";
                message = "The provided session token is invalid or has expired.";
                log.warn("Session token validation failed: {}", iste.getMessage());
            }
            case RateLimitExceededException rle -> {
                status = HttpStatus.TOO_MANY_REQUESTS;
                error = "Rate limit exceeded";
                message = "You have exceeded the allowed request rate. Please retry later.";
                log.warn("Rate limit exceeded for client: {}", rle.clientId());
            }
            case RouteNotFoundException rne -> {
                status = HttpStatus.NOT_FOUND;
                error = "Route not found";
                message = "The requested resource path does not exist.";
                log.info("No route found for path: {}", rne.requestPath());
            }
            case IllegalArgumentException iae -> {
                status = HttpStatus.BAD_REQUEST;
                error = "Bad request";
                message = "The request is malformed or missing required parameters.";
                log.warn("Bad request: {}", iae.getMessage());
            }
            case GatewayDomainException gde -> {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                error = "Internal error";
                message = "An unexpected error occurred. Please contact support.";
                log.error("Unhandled domain exception: {}", gde.getMessage(), gde);
            }
            default -> {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                error = "Internal error";
                message = "An unexpected error occurred. Please contact support.";
                log.error("Unexpected exception in gateway pipeline", ex);
            }
        }

        return writeJsonErrorResponse(exchange, status, error, message);
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Writes a standardized JSON error response directly to the exchange.
     * <strong>Never</strong> includes stack traces or internal details.
     */
    private Mono<Void> writeJsonErrorResponse(ServerWebExchange exchange,
                                               HttpStatus status,
                                               String error,
                                               String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(new IllegalStateException(
                    "Response already committed — cannot write error response"));
        }

        String jsonBody = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s"}"""
                .formatted(
                        Instant.now().toString(),
                        status.value(),
                        escapeJson(error),
                        escapeJson(message));

        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Minimal JSON string escaping for known-safe error messages.
     * Escapes quotes and backslashes only — sufficient for controlled,
     * non-user-supplied error text.
     */
    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
