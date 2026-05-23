package com.finapp.auth.domain.exception;

/**
 * Thrown when a registration attempt is rejected because the email
 * address is already associated with an existing account.
 */
public final class DuplicateEmailException extends AuthDomainException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("An account with this email already exists");
        this.email = email;
    }

    public String email() { return email; }
}
