package com.finapp.transactions.adapter.input.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;

/**
 * Zero Trust JWT Authentication Filter.
 *
 * <p>
 * Intercepts every incoming HTTP request and mathematically validates the
 * JWT signature using the active public key provisioned dynamically by the
 * API Gateway through the {@link IGatewayKeyProvider} port.
 * If valid, the {@code userId} claim is injected into the request context
 * for downstream consumption by controllers. If invalid or missing,
 * the filter executes a fail-secure response (401 Unauthorized).
 * </p>
 *
 * <p><strong>Decentralized Perimeter Validation:</strong> this filter
 * performs cryptographic verification locally in-process (CPU/RAM) without
 * making any network call at request time. The public key is resolved
 * through the {@link IGatewayKeyProvider} abstraction, which returns a
 * pre-cached key with near-zero latency.</p>
 *
 * <p>
 * This filter runs before all other business filters ({@link Order} = 1).
 * </p>
 *
 * @see IGatewayKeyProvider
 */
@Component
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "X-Internal-Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final IGatewayKeyProvider keyProvider;

    /**
     * Constructs the filter with the injected key provider port.
     *
     * <p>The concrete implementation of {@link IGatewayKeyProvider} is
     * resolved by Spring's DI container from the infrastructure layer,
     * preserving the Dependency Inversion Principle.</p>
     *
     * @param keyProvider the gateway key provider (never {@code null})
     */
    public JwtAuthenticationFilter(IGatewayKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Allow health/actuator endpoints without authentication
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = parseClaims(token);
            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                sendUnauthorized(response, "JWT does not contain a valid subject (userId)");
                return;
            }

            request.setAttribute("userId", userId);
            log.debug("Authenticated userId={} for path={}", userId, path);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed for path={}: {}", path, ex.getMessage());
            sendUnauthorized(response, "Invalid or expired JWT token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /*
     * ────────────────────────────────────────────────────────
     * Private helpers
     * ────────────────────────────────────────────────────────
     */

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"%s\"}".formatted(message));
    }

    private Claims parseClaims(String token) {
        PublicKey activeKey = keyProvider.getActivePublicKey();

        return Jwts.parser()
                .verifyWith(activeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
