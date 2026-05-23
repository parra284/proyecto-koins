package com.finapp.gateway.application.usecase;

import com.finapp.gateway.application.port.IRateLimiterRepository;
import com.finapp.gateway.domain.exception.RateLimitExceededException;
import com.finapp.gateway.domain.valueobject.RateLimitRule;

import java.util.Objects;

/**
 * Command Interactor — enforces per-client rate limiting using the
 * Token Bucket algorithm.
 *
 * <p>This interactor encapsulates the rate-limiting business decision:
 * given a client identifier and the configured {@link RateLimitRule},
 * it delegates the atomic token consumption to the
 * {@link IRateLimiterRepository} port and raises a domain exception
 * when the limit is breached.</p>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class EnforceRateLimiting {

    private final IRateLimiterRepository rateLimiterRepository;
    private final RateLimitRule defaultRule;

    /**
     * @param rateLimiterRepository the port for atomic token-bucket operations
     * @param defaultRule           the default rate-limit configuration applied
     *                              when no client-specific rule exists
     */
    public EnforceRateLimiting(IRateLimiterRepository rateLimiterRepository,
                               RateLimitRule defaultRule) {
        this.rateLimiterRepository = Objects.requireNonNull(
                rateLimiterRepository, "rateLimiterRepository must not be null");
        this.defaultRule = Objects.requireNonNull(
                defaultRule, "defaultRule must not be null");
    }

    /**
     * Attempts to allow a request from the given client.
     *
     * <p>If the client's token bucket has available capacity, one token is
     * consumed and the method returns normally. If the bucket is exhausted,
     * a {@link RateLimitExceededException} is thrown.</p>
     *
     * @param clientId the unique identifier of the requesting client
     *                 (typically the userId or remote IP)
     * @throws RateLimitExceededException if the client has exceeded
     *                                    the configured rate limit
     * @throws NullPointerException       if {@code clientId} is null
     * @throws IllegalArgumentException   if {@code clientId} is blank
     */
    public void execute(String clientId) {
        Objects.requireNonNull(clientId, "clientId must not be null");
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }

        boolean allowed = rateLimiterRepository.tryConsume(clientId, defaultRule);

        if (!allowed) {
            throw new RateLimitExceededException(clientId);
        }
    }
}
