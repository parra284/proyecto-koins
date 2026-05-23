package com.finapp.auth.adapter.input.presenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter — transforms security-critical domain events into structured
 * log entries for the observability subsystem (ELK, Datadog, Splunk, etc.).
 *
 * <p>All security events are emitted at the appropriate severity level
 * with a consistent JSON-friendly structure, enabling automated alerting
 * and forensic analysis.</p>
 */
public class SecurityAuditPresenter {

    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Immutable event structure for security audit entries.
     *
     * @param eventType the canonical event type identifier
     * @param userId    the affected user's UUID (nullable for anonymous events)
     * @param email     the affected user's email (nullable)
     * @param timestamp the event timestamp
     * @param severity  the severity level (INFO, WARN, CRITICAL)
     * @param metadata  additional context key-value pairs
     */
    public record SecurityEvent(
            String eventType,
            String userId,
            String email,
            Instant timestamp,
            String severity,
            Map<String, String> metadata
    ) {
        public SecurityEvent {
            Objects.requireNonNull(eventType, "eventType must not be null");
            Objects.requireNonNull(timestamp, "timestamp must not be null");
            Objects.requireNonNull(severity, "severity must not be null");
            metadata = metadata == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(metadata);
        }
    }

    /**
     * Records a successful authentication event.
     */
    public SecurityEvent presentSuccessfulLogin(String userId, String email) {
        SecurityEvent event = new SecurityEvent(
                "AUTH_LOGIN_SUCCESS", userId, email, Instant.now(),
                "INFO", Map.of());
        auditLog.info("[{}] userId={} email={}", event.eventType(), userId, email);
        return event;
    }

    /**
     * Records a failed authentication attempt.
     */
    public SecurityEvent presentFailedLogin(String email, int attemptCount) {
        SecurityEvent event = new SecurityEvent(
                "AUTH_LOGIN_FAILED", null, email, Instant.now(),
                "WARN", Map.of("attemptCount", String.valueOf(attemptCount)));
        auditLog.warn("[{}] email={} attemptCount={}", event.eventType(), email, attemptCount);
        return event;
    }

    /**
     * Records an account lockout event (critical security alert).
     */
    public SecurityEvent presentAccountLocked(String userId, String email) {
        SecurityEvent event = new SecurityEvent(
                "AUTH_ACCOUNT_LOCKED", userId, email, Instant.now(),
                "CRITICAL", Map.of("action", "BRUTE_FORCE_LOCKOUT"));
        auditLog.error("[{}] userId={} email={} — account locked due to brute-force",
                event.eventType(), userId, email);
        return event;
    }

    /**
     * Records a token revocation event.
     */
    public SecurityEvent presentTokenRevocation(String userId, String tokenId) {
        SecurityEvent event = new SecurityEvent(
                "AUTH_TOKEN_REVOKED", userId, null, Instant.now(),
                "INFO", Map.of("tokenId", tokenId));
        auditLog.info("[{}] userId={} tokenId={}", event.eventType(), userId, tokenId);
        return event;
    }

    /**
     * Records a new user registration event.
     */
    public SecurityEvent presentRegistration(String userId, String email) {
        SecurityEvent event = new SecurityEvent(
                "AUTH_USER_REGISTERED", userId, email, Instant.now(),
                "INFO", Map.of());
        auditLog.info("[{}] userId={} email={}", event.eventType(), userId, email);
        return event;
    }
}
