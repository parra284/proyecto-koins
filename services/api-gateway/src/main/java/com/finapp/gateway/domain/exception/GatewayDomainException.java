package com.finapp.gateway.domain.exception;

/**
 * Base unchecked exception for all domain-level errors in the API Gateway.
 *
 * <p>Subclasses represent specific failure modes that the adapter layer
 * can intercept and translate into proper HTTP responses.</p>
 *
 * <p><strong>Framework-free:</strong> Pure Java exception hierarchy.</p>
 */
public class GatewayDomainException extends RuntimeException {

    public GatewayDomainException(String message) {
        super(message);
    }

    public GatewayDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
