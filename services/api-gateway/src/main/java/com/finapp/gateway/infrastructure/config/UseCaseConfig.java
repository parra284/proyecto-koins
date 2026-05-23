package com.finapp.gateway.infrastructure.config;

import com.finapp.gateway.adapter.input.GatewayRequestInterceptor;
import com.finapp.gateway.adapter.output.http.ReverseProxyHttpAdapter;
import com.finapp.gateway.adapter.output.jwt.ExternalJwtSessionAdapter;
import com.finapp.gateway.adapter.output.jwt.InternalJwtSignerAdapter;
import com.finapp.gateway.adapter.output.redis.RedisRateLimiterAdapter;
import com.finapp.gateway.adapter.output.telemetry.GatewayMetricsAndTracingAdapter;
import com.finapp.gateway.application.port.IApplicationTokenSigner;
import com.finapp.gateway.application.port.IGatewayTelemetry;
import com.finapp.gateway.application.port.IPublicKeyProvider;
import com.finapp.gateway.application.port.IRateLimiterRepository;
import com.finapp.gateway.application.port.IRouteMappingRepository;
import com.finapp.gateway.application.port.ISessionTokenValidator;
import com.finapp.gateway.application.usecase.EnforceRateLimiting;
import com.finapp.gateway.application.usecase.ExchangeSessionTokenForApplicationToken;
import com.finapp.gateway.domain.valueobject.RateLimitRule;
import com.finapp.gateway.infrastructure.jwks.AuthServicePublicKeyFetcher;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PrivateKey;
import java.time.Duration;

/**
 * Spring configuration — wires application-layer interactors, adapter
 * implementations, and infrastructure components via constructor injection,
 * bridging the Dependency Inversion boundary.
 *
 * <p>The interactors and domain classes are pure Java with no Spring
 * annotations. This configuration is the single point where Spring's DI
 * container supplies the infrastructure implementations of the ports.</p>
 */
@Configuration
public class UseCaseConfig {

    /* ────────────────────────────────────────────────────────
       Infrastructure: JWKS Key Provider (Hot Cache)
       ──────────────────────────────────────────────────────── */

    @Bean(destroyMethod = "destroy")
    public AuthServicePublicKeyFetcher authServicePublicKeyFetcher(
            WebClient.Builder webClientBuilder,
            @Value("${gateway.security.auth-jwks-url}") String jwksUrl,
            @Value("${gateway.security.jwks-refresh-interval-minutes:5}") int refreshMinutes) {
        return new AuthServicePublicKeyFetcher(
                webClientBuilder, jwksUrl, Duration.ofMinutes(refreshMinutes));
    }

    /* ────────────────────────────────────────────────────────
       Port Implementations (Adapters)
       ──────────────────────────────────────────────────────── */

    @Bean
    public IPublicKeyProvider publicKeyProvider(AuthServicePublicKeyFetcher fetcher) {
        return fetcher;
    }

    @Bean
    public ISessionTokenValidator sessionTokenValidator(IPublicKeyProvider publicKeyProvider) {
        return new ExternalJwtSessionAdapter(publicKeyProvider);
    }

    @Bean
    public IApplicationTokenSigner applicationTokenSigner(PrivateKey gatewayPrivateKey) {
        return new InternalJwtSignerAdapter(gatewayPrivateKey);
    }

    @Bean
    public IRateLimiterRepository rateLimiterRepository(ReactiveStringRedisTemplate redisTemplate) {
        return new RedisRateLimiterAdapter(redisTemplate);
    }

    @Bean
    public IGatewayTelemetry gatewayTelemetry(MeterRegistry meterRegistry) {
        return new GatewayMetricsAndTracingAdapter(meterRegistry);
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

    /* ────────────────────────────────────────────────────────
       Network Adapters
       ──────────────────────────────────────────────────────── */

    @Bean
    public ReverseProxyHttpAdapter reverseProxyHttpAdapter(WebClient.Builder webClientBuilder) {
        return new ReverseProxyHttpAdapter(webClientBuilder);
    }

    /* ────────────────────────────────────────────────────────
       Intercepting Filter (WebFilter)
       ──────────────────────────────────────────────────────── */

    @Bean
    public GatewayRequestInterceptor gatewayRequestInterceptor(
            IRouteMappingRepository routeMappingRepository,
            EnforceRateLimiting enforceRateLimiting,
            ExchangeSessionTokenForApplicationToken exchangeToken,
            ReverseProxyHttpAdapter reverseProxy,
            IGatewayTelemetry telemetry) {
        return new GatewayRequestInterceptor(
                routeMappingRepository,
                enforceRateLimiting,
                exchangeToken,
                reverseProxy,
                telemetry);
    }
}
