package com.finapp.gateway.adapter.input;

import com.finapp.gateway.adapter.output.http.ReverseProxyHttpAdapter;
import com.finapp.gateway.application.port.IGatewayTelemetry;
import com.finapp.gateway.application.port.IRouteMappingRepository;
import com.finapp.gateway.application.usecase.EnforceRateLimiting;
import com.finapp.gateway.application.usecase.ExchangeSessionTokenForApplicationToken;
import com.finapp.gateway.domain.entity.RouteMapping;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;
import java.util.Optional;

/**
 * Intercepting Filter — the perimeter entry point for all gateway traffic.
 *
 * <p>Implements the <em>Front Controller / Intercepting Filter Pattern</em>
 * as a reactive {@link WebFilter}. This component does <strong>not</strong>
 * expose endpoints of its own; it intercepts all incoming HTTP flows and
 * executes the gateway pipeline for matched routes:</p>
 *
 * <ol>
 *   <li><strong>Route resolution</strong> — determines the downstream target
 *       via {@link IRouteMappingRepository}. Unmatched paths are passed
 *       through to the filter chain (actuator, JWKS, etc.).</li>
 *   <li><strong>Trace propagation</strong> — generates or propagates
 *       the distributed trace ID.</li>
 *   <li><strong>Rate limiting</strong> — enforces Token Bucket quotas.</li>
 *   <li><strong>Token exchange</strong> — (if the route requires
 *       authentication) validates the session token and mints an internal
 *       application JWT.</li>
 *   <li><strong>Dispatch</strong> — forwards the mutated request to the
 *       target service via {@link ReverseProxyHttpAdapter}.</li>
 * </ol>
 *
 * <p>Blocking operations (Redis rate-limiting, JJWT parsing) are offloaded
 * to {@code Schedulers.boundedElastic()} to avoid stalling the Netty
 * event loop.</p>
 */
public class GatewayRequestInterceptor implements WebFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final IRouteMappingRepository routeMappingRepository;
    private final EnforceRateLimiting enforceRateLimiting;
    private final ExchangeSessionTokenForApplicationToken exchangeToken;
    private final ReverseProxyHttpAdapter reverseProxy;
    private final IGatewayTelemetry telemetry;

    public GatewayRequestInterceptor(
            IRouteMappingRepository routeMappingRepository,
            EnforceRateLimiting enforceRateLimiting,
            ExchangeSessionTokenForApplicationToken exchangeToken,
            ReverseProxyHttpAdapter reverseProxy,
            IGatewayTelemetry telemetry) {
        this.routeMappingRepository = Objects.requireNonNull(routeMappingRepository);
        this.enforceRateLimiting = Objects.requireNonNull(enforceRateLimiting);
        this.exchangeToken = Objects.requireNonNull(exchangeToken);
        this.reverseProxy = Objects.requireNonNull(reverseProxy);
        this.telemetry = Objects.requireNonNull(telemetry);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        long startNanos = System.nanoTime();

        String traceId = telemetry.propagateTraceId(
                request.getHeaders().getFirst(TRACE_ID_HEADER));

        Optional<RouteMapping> routeOpt = routeMappingRepository.findByPath(path);
        if (routeOpt.isEmpty()) {
            return chain.filter(exchange);
        }

        RouteMapping route = routeOpt.get();
        String method = request.getMethod().name();

        return Mono.fromCallable(() -> {
                    // ── 1. Rate Limiting ──────────────────────────────────
                    String clientId = extractClientId(request);
                    enforceRateLimiting.execute(clientId);

                    // ── 2. Token Exchange (conditional) ───────────────────
                    if (route.requiresAuthentication()) {
                        String sessionToken = extractBearerToken(request);
                        return Optional.of(exchangeToken.execute(sessionToken).applicationToken());
                    }
                    return Optional.<String>empty();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optToken ->
                        reverseProxy.dispatch(exchange, route, optToken.orElse(null), traceId))
                .doOnSuccess(v -> {
                    long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                    telemetry.recordRequestRouted(route.publicPathPattern(), method, durationMs);
                })
                .doOnError(ex ->
                        telemetry.recordRequestBlocked(ex.getClass().getSimpleName()));
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Extracts the Bearer token from the Authorization header.
     *
     * @throws IllegalArgumentException if the header is missing or malformed
     */
    private String extractBearerToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException(
                    "Missing or malformed Authorization header — expected 'Bearer <token>'");
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Derives a client identifier for rate-limiting purposes.
     * Prefers a hash of the Bearer token; falls back to the remote IP.
     */
    private String extractClientId(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return "bearer:" + Integer.toHexString(authHeader.hashCode());
        }
        if (request.getRemoteAddress() != null) {
            return "ip:" + request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
