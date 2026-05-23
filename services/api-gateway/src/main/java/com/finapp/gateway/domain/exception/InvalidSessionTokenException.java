package com.finapp.gateway.domain.exception;

/**
 * Thrown when an incoming session token is expired, malformed, or
 * fails cryptographic verification against the Authentication service's
 * public key.
 */
public final class InvalidSessionTokenException extends GatewayDomainException {

    public InvalidSessionTokenException(String message) {
        super(message);
    }

    public InvalidSessionTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
