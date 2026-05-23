package com.finapp.auth.domain.exception;

/**
 * Thrown when an email address does not conform to the RFC 5322 syntax
 * requirements enforced by the {@link com.finapp.auth.domain.valueobject.Credentials}
 * Value Object.
 */
public final class InvalidEmailException extends AuthDomainException {

    public InvalidEmailException(String message) {
        super(message);
    }
}
