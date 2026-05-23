package com.finapp.gateway.application.port;

import com.finapp.gateway.domain.valueobject.RateLimitRule;

/**
 * Output Port — abstracts the storage and atomic consumption of
 * Token Bucket counters for rate limiting.
 *
 * <p>Implementations reside in the adapter layer and typically
 * leverage Redis atomic commands or Lua scripts for consistency
 * under concurrent access.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IRateLimiterRepository {

    /**
     * Attempts to consume one token from the bucket identified by
     * {@code clientId}, applying the given {@code rule} parameters.
     *
     * <p>The implementation must atomically:</p>
     * <ol>
     *   <li>Refill tokens based on elapsed time and {@link RateLimitRule#refillRatePerSecond()}.</li>
     *   <li>Check if at least one token is available.</li>
     *   <li>Decrement the counter if a token is available.</li>
     * </ol>
     *
     * @param clientId the unique identifier of the client (e.g., userId or IP)
     * @param rule     the rate-limit configuration to apply
     * @return {@code true} if a token was consumed (request allowed),
     *         {@code false} if the bucket is empty (request denied)
     */
    boolean tryConsume(String clientId, RateLimitRule rule);
}
