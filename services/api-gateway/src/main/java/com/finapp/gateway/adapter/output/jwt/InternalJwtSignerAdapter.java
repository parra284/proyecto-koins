package com.finapp.gateway.adapter.output.jwt;

import com.finapp.gateway.application.port.IApplicationTokenSigner;
import com.finapp.gateway.domain.valueobject.UserIdentityContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapter — signs short-lived internal application JWTs using the
 * Gateway's RSA private key (RS256).
 *
 * <p>Implements {@link IApplicationTokenSigner}. The generated token
 * is valid for exactly <strong>1 minute</strong>, following the Zero-Trust
 * principle of least privilege and minimal time-window exposure.</p>
 *
 * <p>The private key is injected at construction time from the
 * infrastructure configuration (loaded from vault / environment).</p>
 */
public class InternalJwtSignerAdapter implements IApplicationTokenSigner {

    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(1);
    private static final String ISSUER = "api-gateway";
    private static final String CLAIMS_ROLES_KEY = "roles";

    private final PrivateKey gatewayPrivateKey;

    /**
     * @param gatewayPrivateKey the RSA private key owned by the Gateway,
     *                          used to sign internal application tokens
     */
    public InternalJwtSignerAdapter(PrivateKey gatewayPrivateKey) {
        this.gatewayPrivateKey = Objects.requireNonNull(
                gatewayPrivateKey, "gatewayPrivateKey must not be null");
    }

    /**
     * Signs a new application JWT embedding the user's identity and roles.
     *
     * <p>Token structure:</p>
     * <ul>
     *   <li><strong>sub</strong> — the authenticated userId</li>
     *   <li><strong>roles</strong> — the list of authorization roles</li>
     *   <li><strong>iss</strong> — "api-gateway"</li>
     *   <li><strong>iat</strong> — issued-at timestamp</li>
     *   <li><strong>exp</strong> — expiration (issued-at + 1 minute)</li>
     *   <li><strong>jti</strong> — unique token ID (UUID) for replay detection</li>
     * </ul>
     *
     * @param identity the validated user identity context
     * @return the signed JWT string
     */
    @Override
    public String sign(UserIdentityContext identity) {
        Objects.requireNonNull(identity, "identity must not be null");

        Instant now = Instant.now();
        Instant expiration = now.plus(TOKEN_VALIDITY);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(identity.userId())
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim(CLAIMS_ROLES_KEY, identity.roles())
                .signWith(gatewayPrivateKey, Jwts.SIG.RS256)
                .compact();
    }
}
