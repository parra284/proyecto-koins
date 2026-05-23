package com.finapp.gateway.infrastructure.config;

import com.finapp.gateway.adapter.output.jwt.ExternalJwtSessionAdapter;
import com.finapp.gateway.adapter.output.jwt.InternalJwtSignerAdapter;
import com.finapp.gateway.adapter.output.redis.RedisRateLimiterAdapter;
import com.finapp.gateway.application.port.IApplicationTokenSigner;
import com.finapp.gateway.application.port.IRateLimiterRepository;
import com.finapp.gateway.application.port.IRouteMappingRepository;
import com.finapp.gateway.application.port.ISessionTokenValidator;
import com.finapp.gateway.application.usecase.EnforceRateLimiting;
import com.finapp.gateway.application.usecase.ExchangeSessionTokenForApplicationToken;
import com.finapp.gateway.application.usecase.RouteIncomingRequest;
import com.finapp.gateway.domain.valueobject.RateLimitRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Spring configuration — wires application-layer interactors and adapter
 * implementations via constructor injection, bridging the Dependency
 * Inversion boundary.
 *
 * <p>The interactors and domain classes are pure Java with no Spring
 * annotations. This configuration is the single point where Spring's DI
 * container supplies the infrastructure implementations of the ports.</p>
 */
@Configuration
public class UseCaseConfig {

    /* ────────────────────────────────────────────────────────
       Port Implementations (Adapters)
       ──────────────────────────────────────────────────────── */

    @Bean
    public ISessionTokenValidator sessionTokenValidator(PublicKey authenticationPublicKey) {
        return new ExternalJwtSessionAdapter(authenticationPublicKey);
    }

    @Bean
    public IApplicationTokenSigner applicationTokenSigner(PrivateKey gatewayPrivateKey) {
        return new InternalJwtSignerAdapter(gatewayPrivateKey);
    }

    @Bean
    public IRateLimiterRepository rateLimiterRepository(ReactiveStringRedisTemplate redisTemplate) {
        return new RedisRateLimiterAdapter(redisTemplate);
    }

    /* ────────────────────────────────────────────────────────
       Domain Configuration
       ──────────────────────────────────────────────────────── */

    @Bean
    public RateLimitRule defaultRateLimitRule(
            @Value("${gateway.rate-limit.bucket-capacity:100}") Integer bucketCapacity,
            @Value("${gateway.rate-limit.refill-rate-per-second:10}") Integer refillRate) {
        return new RateLimitRule(bucketCapacity, refillRate);
    }

    /* ────────────────────────────────────────────────────────
       Interactors (Use Cases)
       ──────────────────────────────────────────────────────── */

    @Bean
    public ExchangeSessionTokenForApplicationToken exchangeSessionTokenForApplicationToken(
            ISessionTokenValidator sessionTokenValidator,
            IApplicationTokenSigner applicationTokenSigner) {
        return new ExchangeSessionTokenForApplicationToken(
                sessionTokenValidator, applicationTokenSigner);
    }

    @Bean
    public EnforceRateLimiting enforceRateLimiting(
            IRateLimiterRepository rateLimiterRepository,
            RateLimitRule defaultRateLimitRule) {
        return new EnforceRateLimiting(rateLimiterRepository, defaultRateLimitRule);
    }

    @Bean
    public RouteIncomingRequest routeIncomingRequest(
            IRouteMappingRepository routeMappingRepository) {
        return new RouteIncomingRequest(routeMappingRepository);
    }
}
