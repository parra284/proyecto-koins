package com.finapp.transactions.adapter.input.filter;

import java.security.PublicKey;

/**
 * Gateway Key Provider — adapter-layer port for JWT key resolution.
 *
 * <p>
 * Defines the contract through which the {@link JwtAuthenticationFilter}
 * obtains the active public key for cryptographic JWT verification.
 * This abstraction enforces the Dependency Inversion Principle: the
 * security filter depends only on this interface, never on a concrete
 * HTTP client, file reader, or any infrastructure detail.
 * </p>
 *
 * <p><strong>Clean Architecture boundary:</strong> this interface lives in
 * the adapter layer (Layer 3) alongside the filter that consumes it.
 * The concrete implementation resides in the infrastructure layer (Layer 4)
 * and is injected by the Spring DI container at runtime.</p>
 *
 * @see JwtAuthenticationFilter
 */
public interface IGatewayKeyProvider {

    /**
     * Returns the currently active public key provisioned by the API Gateway.
     *
     * <p>The returned key is used to mathematically verify the asymmetric
     * signature (RS256) of incoming JWT tokens. Implementations are expected
     * to cache the key in memory and return it with near-zero latency.</p>
     *
     * @return the active RSA {@link PublicKey}, never {@code null}
     * @throws IllegalStateException if no key has been loaded or the
     *                               provider failed to initialize
     */
    PublicKey getActivePublicKey();
}
