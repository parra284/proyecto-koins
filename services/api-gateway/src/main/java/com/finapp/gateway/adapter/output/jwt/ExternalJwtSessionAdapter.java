package com.finapp.gateway.adapter.output.jwt;

import com.finapp.gateway.application.port.IPublicKeyProvider;
import com.finapp.gateway.application.port.ISessionTokenValidator;
import com.finapp.gateway.domain.exception.InvalidSessionTokenException;
import com.finapp.gateway.domain.valueobject.UserIdentityContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.PublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Adapter — validates incoming session tokens using a dynamically
 * provisioned public key from the {@link IPublicKeyProvider} port.
 *
 * <p>Implements {@link ISessionTokenValidator} and translates JJWT
 * exceptions into the domain's {@link InvalidSessionTokenException}.</p>
 *
 * <p><strong>Key rotation support:</strong> The public key is fetched
 * from the provider on each validation call. Since the provider is
 * backed by a hot cache ({@code AuthServicePublicKeyFetcher}), this
 * is effectively a local memory read with sub-millisecond latency,
 * while transparently supporting key rotation.</p>
 */
public class ExternalJwtSessionAdapter implements ISessionTokenValidator {

    private static final String CLAIMS_ROLES_KEY = "roles";
    private static final String CLAIMS_USER_ID_KEY = "sub";
    private static final Duration KEY_FETCH_TIMEOUT = Duration.ofSeconds(5);

    private final IPublicKeyProvider publicKeyProvider;

    /**
     * @param publicKeyProvider the port providing the Authentication service's
     *                          current public key for signature verification
     */
    public ExternalJwtSessionAdapter(IPublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = Objects.requireNonNull(
                publicKeyProvider, "publicKeyProvider must not be null");
    }

    /**
     * Parses and validates the session token's signature, expiration, and claims.
     *
     * <p>The public key is retrieved from the {@link IPublicKeyProvider} hot cache.
     * The {@code .block()} call is safe here because the provider serves from
     * an in-memory {@code AtomicReference} — no network I/O occurs on the hot path.</p>
     *
     * @param sessionToken the raw JWT string
     * @return a {@link UserIdentityContext} extracted from the token's claims
     * @throws InvalidSessionTokenException if verification fails for any reason
     */
    @Override
    public UserIdentityContext validate(String sessionToken) {
        PublicKey authenticationPublicKey = publicKeyProvider.getPublicKey()
                .block(KEY_FETCH_TIMEOUT);

        if (authenticationPublicKey == null) {
            throw new InvalidSessionTokenException(
                    "Authentication public key unavailable — cannot verify session token");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(authenticationPublicKey)
                    .build()
                    .parseSignedClaims(sessionToken)
                    .getPayload();

            String userId = claims.get(CLAIMS_USER_ID_KEY, String.class);
            if (userId == null || userId.isBlank()) {
                throw new InvalidSessionTokenException(
                        "Session token missing required 'sub' claim");
            }

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(CLAIMS_ROLES_KEY, List.class);
            if (roles == null || roles.isEmpty()) {
                throw new InvalidSessionTokenException(
                        "Session token missing required 'roles' claim");
            }

            return new UserIdentityContext(userId, roles);

        } catch (ExpiredJwtException ex) {
            throw new InvalidSessionTokenException(
                    "Session token has expired: " + ex.getMessage(), ex);
        } catch (JwtException ex) {
            throw new InvalidSessionTokenException(
                    "Session token verification failed: " + ex.getMessage(), ex);
        }
    }
}
