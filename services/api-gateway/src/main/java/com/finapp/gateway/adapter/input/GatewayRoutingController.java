package com.finapp.gateway.adapter.input;

import com.finapp.gateway.application.usecase.EnforceRateLimiting;
import com.finapp.gateway.application.usecase.ExchangeSessionTokenForApplicationToken;
import com.finapp.gateway.application.usecase.RouteIncomingRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Front Controller — the single HTTP entry point for all gateway traffic.
 *
 * <p>Intercepts every incoming request and executes the gateway pipeline
 * sequentially:</p>
 * <ol>
 *   <li><strong>Route resolution</strong> — determines the downstream target.</li>
 *   <li><strong>Rate limiting</strong> — enforces Token Bucket quotas.</li>
 *   <li><strong>Token exchange</strong> — (if the route requires authentication)
 *       validates the session token and mints an internal application JWT.</li>
 *   <li><strong>Dispatch</strong> — forwards the request to the target service,
 *       injecting the application token in a {@code X-Internal-Token} header.</li>
 * </ol>
 *
 * <p>This controller is the <em>only</em> Spring-annotated class that
 * touches the application layer interactors.</p>
 */
@RestController
public class GatewayRoutingController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final ExchangeSessionTokenForApplicationToken exchangeToken;
    private final EnforceRateLimiting enforceRateLimiting;
    private final RouteIncomingRequest routeIncomingRequest;
    private final WebClient webClient;

    public GatewayRoutingController(
            ExchangeSessionTokenForApplicationToken exchangeToken,
            EnforceRateLimiting enforceRateLimiting,
            RouteIncomingRequest routeIncomingRequest,
            WebClient.Builder webClientBuilder) {
        this.exchangeToken = exchangeToken;
        this.enforceRateLimiting = enforceRateLimiting;
        this.routeIncomingRequest = routeIncomingRequest;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Catch-all mapping — every request path is handled by the gateway pipeline.
     */
    @RequestMapping("/**")
    public Mono<byte[]> handleRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getPath().value();
        HttpMethod method = request.getMethod();

        // ── 1. Route Resolution ──────────────────────────────
        RouteIncomingRequest.Result routeResult = routeIncomingRequest.execute(requestPath);

        // ── 2. Rate Limiting (by IP fallback if unauthenticated) ──
        String clientId = extractClientId(request);
        enforceRateLimiting.execute(clientId);

        // ── 3. Token Exchange (conditional) ──────────────────
        String internalToken = null;
        if (routeResult.route().requiresAuthentication()) {
            String sessionToken = extractBearerToken(request);
            ExchangeSessionTokenForApplicationToken.Result tokenResult =
                    exchangeToken.execute(sessionToken);
            internalToken = tokenResult.applicationToken();
        }

        // ── 4. Dispatch to downstream service ────────────────
        String targetUrl = routeResult.targetUrl();
        final String tokenToForward = internalToken;

        return webClient
                .method(method)
                .uri(targetUrl)
                .headers(headers -> {
                    // Forward original headers (excluding Authorization)
                    request.getHeaders().forEach((name, values) -> {
                        if (!AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
                            headers.addAll(name, values);
                        }
                    });
                    // Inject internal application token
                    if (tokenToForward != null) {
                        headers.set(INTERNAL_TOKEN_HEADER, BEARER_PREFIX + tokenToForward);
                    }
                })
                .body(Mono.justOrEmpty(request.getBody()), byte[].class)
                .retrieve()
                .bodyToMono(byte[].class);
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
     * Prefers the authenticated userId from the Bearer token; falls back
     * to the remote IP address for unauthenticated routes.
     */
    private String extractClientId(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            // Use a hash of the token as a lightweight client key
            // (full validation happens later in the pipeline)
            return "bearer:" + Integer.toHexString(authHeader.hashCode());
        }
        // Fallback to remote address
        if (request.getRemoteAddress() != null) {
            return "ip:" + request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
