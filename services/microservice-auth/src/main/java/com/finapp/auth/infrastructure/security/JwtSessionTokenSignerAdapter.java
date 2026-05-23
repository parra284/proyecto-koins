package com.finapp.auth.infrastructure.security;

import com.finapp.auth.application.port.IKeyProvider;
import com.finapp.auth.application.port.ISessionTokenSigner;
import io.jsonwebtoken.Jwts;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure Adapter — implements {@link ISessionTokenSigner} by
 * signing session JWTs with the Auth service's RSA private key (RS256).
 *
 * <p>The signed token is the external credential that clients present
 * to the API Gateway. Token characteristics:</p>
 * <ul>
 *   <li><strong>Algorithm:</strong> RS256 (RSA-SHA256)</li>
 *   <li><strong>Validity:</strong> 30 minutes (configurable)</li>
 *   <li><strong>Claims:</strong> sub (userId), email, roles, iss, iat, exp, jti</li>
 *   <li><strong>Key rotation:</strong> kid header for JWKS-based rotation</li>
 * </ul>
 */
public class JwtSessionTokenSignerAdapter implements ISessionTokenSigner {

    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(30);
    private static final String ISSUER = "microservice-auth";

    private final IKeyProvider keyProvider;
    private final PrivateKey privateKey;

    public JwtSessionTokenSignerAdapter(IKeyProvider keyProvider) {
        this.keyProvider = Objects.requireNonNull(
                keyProvider, "keyProvider must not be null");
        this.privateKey = loadPrivateKey(keyProvider.privateKeyEncoded());
    }

    /**
     * Signs a session JWT embedding the user's identity, email, and role.
     *
     * @param userId the user's opaque UUID
     * @param email  the user's email address
     * @param role   the user's authorization role
     * @return the compact, signed JWT string
     */
    @Override
    public String sign(String userId, String email, String role) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");

        Instant now = Instant.now();
        Instant expiration = now.plus(TOKEN_VALIDITY);

        return Jwts.builder()
                .header()
                    .keyId(keyProvider.activeKeyId())
                    .and()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("email", email)
                .claim("roles", List.of(role))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    private static PrivateKey loadPrivateKey(byte[] pkcs8Bytes) {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA private key", ex);
        }
    }
}
