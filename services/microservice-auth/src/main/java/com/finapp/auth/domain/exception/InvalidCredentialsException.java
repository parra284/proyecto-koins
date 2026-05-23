package com.finapp.auth.domain.exception;

/**
 * Thrown when authentication fails due to incorrect credentials.
 *
 * <p><strong>Security principle:</strong> This exception uses a generic message
 * that does not reveal whether the email or password was incorrect, preventing
 * user enumeration attacks.</p>
 */
public final class InvalidCredentialsException extends AuthDomainException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
