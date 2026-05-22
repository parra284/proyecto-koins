package com.finapp.transactions.adapter.input.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Zero Trust JWT Authentication Filter.
 *
 * <p>
 * Intercepts every incoming HTTP request and mathematically validates the
 * JWT signature using the public key provisioned by the API Gateway.
 * If valid, the {@code userId} claim is injected into the request context
 * for downstream consumption by controllers. If invalid or missing,
 * the filter executes a fail-secure response (401 Unauthorized).
 * </p>
 *
 * <p>
 * This filter runs before all other business filters ({@link Order} = 1).
 * </p>
 */
// @Component
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final PublicKey publicKey;

    public JwtAuthenticationFilter(@Value("${app.jwt.public-key}") String publicKeyPem) {
        this.publicKey = parsePublicKey(publicKeyPem);
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
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                sendUnauthorized(response, "JWT does not contain a valid subject (userId)");
                return;
            }

            // Inject userId into request context for controllers
            request.setAttribute("userId", userId);
            log.debug("Authenticated userId={} for path={}", userId, path);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.warn("JWT validation failed for path={}: {}", path, ex.getMessage());
            sendUnauthorized(response, "Invalid or expired JWT token");
        }
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

    private static PublicKey parsePublicKey(String pem) {
        try {
            String keyContent = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(keyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JWT public key", e);
        }
    }
}
