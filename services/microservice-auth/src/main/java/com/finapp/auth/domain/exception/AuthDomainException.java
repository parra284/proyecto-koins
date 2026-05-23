package com.finapp.auth.domain.exception;

/**
 * Base unchecked exception for all domain-level errors in the Auth Service.
 *
 * <p>Subclasses represent specific security and business failure modes that
 * the adapter layer intercepts and translates into safe HTTP responses.</p>
 *
 * <p><strong>Framework-free:</strong> Pure Java exception hierarchy.</p>
 */
public class AuthDomainException extends RuntimeException {

    public AuthDomainException(String message) {
        super(message);
    }

    public AuthDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
