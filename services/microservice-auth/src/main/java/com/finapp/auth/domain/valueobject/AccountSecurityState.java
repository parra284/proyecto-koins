package com.finapp.auth.domain.valueobject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object — tracks the security posture of a user account,
 * controlling brute-force lockout transitions.
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>{@code failedLoginAttempts} must be non-negative.</li>
 *   <li>{@code lockoutExpiration} may be {@code null} (unlocked) or a valid timestamp.</li>
 * </ol>
 *
 * <p>This class is <em>completely immutable</em>. All mutation methods return
 * new instances (copy-on-write pattern), preserving referential transparency
 * within the {@link com.finapp.auth.domain.entity.User} Aggregate Root.</p>
 *
 * <p><strong>Framework-free:</strong> No Spring, JPA, or external annotations.</p>
 */
public final class AccountSecurityState {

    /**
     * Maximum consecutive failed login attempts before the account is locked.
     * Banking-grade default: 3 attempts.
     */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    /**
     * Duration of the account lockout period after exceeding the attempt threshold.
     * Banking-grade default: 15 minutes.
     */
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final int failedLoginAttempts;
    private final LocalDateTime lockoutExpiration;

    /**
     * Canonical constructor — validates all invariants.
     *
     * @param failedLoginAttempts the current count of consecutive failed login attempts
     * @param lockoutExpiration   the timestamp at which the lockout expires, or {@code null} if not locked
     * @throws IllegalArgumentException if {@code failedLoginAttempts} is negative
     */
    public AccountSecurityState(int failedLoginAttempts, LocalDateTime lockoutExpiration) {
        if (failedLoginAttempts < 0) {
            throw new IllegalArgumentException(
                    "failedLoginAttempts must be non-negative, got: " + failedLoginAttempts);
        }
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockoutExpiration = lockoutExpiration;
    }

    /**
     * Factory — creates a clean, unlocked security state for newly registered accounts.
     */
    public static AccountSecurityState clean() {
        return new AccountSecurityState(0, null);
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public int failedLoginAttempts()         { return failedLoginAttempts; }
    public LocalDateTime lockoutExpiration()  { return lockoutExpiration; }

    /* ────────────────────────────────────────────────────────
       Lockout evaluation — pure business logic
       ──────────────────────────────────────────────────────── */

    /**
     * Returns {@code true} if the account is currently locked
     * (lockout timestamp exists and has not yet expired).
     */
    public boolean isLocked() {
        return lockoutExpiration != null && LocalDateTime.now().isBefore(lockoutExpiration);
    }

    /**
     * Returns {@code true} if the failed-attempt counter has reached
     * or exceeded the {@link #MAX_FAILED_ATTEMPTS} threshold.
     */
    public boolean hasExceededMaxAttempts() {
        return failedLoginAttempts >= MAX_FAILED_ATTEMPTS;
    }

    /* ────────────────────────────────────────────────────────
       State transitions (copy-on-write — immutable)
       ──────────────────────────────────────────────────────── */

    /**
     * Records a single failed login attempt.
     *
     * @return a new {@link AccountSecurityState} with the attempt counter incremented by one
     */
    public AccountSecurityState recordFailedAttempt() {
        return new AccountSecurityState(this.failedLoginAttempts + 1, this.lockoutExpiration);
    }

    /**
     * Locks the account for the configured {@link #LOCKOUT_DURATION}.
     *
     * @return a new {@link AccountSecurityState} with the lockout timestamp set
     */
    public AccountSecurityState lock() {
        return new AccountSecurityState(
                this.failedLoginAttempts,
                LocalDateTime.now().plus(LOCKOUT_DURATION));
    }

    /**
     * Resets the security state to a clean, unlocked position.
     * Used after a successful authentication.
     *
     * @return a new clean {@link AccountSecurityState}
     */
    public AccountSecurityState resetAttempts() {
        return AccountSecurityState.clean();
    }

    /* ────────────────────────────────────────────────────────
       Equality by value
       ──────────────────────────────────────────────────────── */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountSecurityState that)) return false;
        return failedLoginAttempts == that.failedLoginAttempts
                && Objects.equals(lockoutExpiration, that.lockoutExpiration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(failedLoginAttempts, lockoutExpiration);
    }

    @Override
    public String toString() {
        return "AccountSecurityState[failedAttempts=%d, lockoutExpiration=%s, isLocked=%s]"
                .formatted(failedLoginAttempts, lockoutExpiration, isLocked());
    }
}
