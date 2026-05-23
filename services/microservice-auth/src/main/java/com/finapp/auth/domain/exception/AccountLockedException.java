package com.finapp.auth.domain.exception;

/**
 * Thrown when a login attempt is rejected because the account has been
 * locked due to excessive failed authentication attempts.
 *
 * <p>The exception deliberately does not reveal lockout timing details
 * to prevent information leakage to potential attackers.</p>
 */
public final class AccountLockedException extends AuthDomainException {

    private final String email;

    public AccountLockedException(String email) {
        super("Account is temporarily locked due to excessive failed login attempts");
        this.email = email;
    }

    public String email() { return email; }
}
