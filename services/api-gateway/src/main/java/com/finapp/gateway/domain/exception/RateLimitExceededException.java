package com.finapp.gateway.domain.exception;

/**
 * Thrown when a client has exhausted its Token Bucket allowance and must
 * wait before sending additional requests.
 */
public final class RateLimitExceededException extends GatewayDomainException {

    private final String clientId;

    public RateLimitExceededException(String clientId) {
        super("Rate limit exceeded for client: " + clientId);
        this.clientId = clientId;
    }

    public String clientId() { return clientId; }
}
