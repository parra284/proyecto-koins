package com.finapp.gateway.adapter.output.jwt;

import com.finapp.gateway.application.port.ISessionTokenValidator;
import com.finapp.gateway.domain.exception.InvalidSessionTokenException;
import com.finapp.gateway.domain.valueobject.UserIdentityContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.PublicKey;
import java.util.List;
import java.util.Objects;

/**
 * Adapter — validates incoming session tokens using the Authentication
 * service's RSA public key via the JJWT library.
 *
 * <p>Implements {@link ISessionTokenValidator} and translates JJWT
 * exceptions into the domain's {@link InvalidSessionTokenException}.</p>
 *
 * <p>The public key is injected at construction time from the
 * infrastructure configuration (loaded from vault / environment).</p>
 */
public class ExternalJwtSessionAdapter implements ISessionTokenValidator {

    private static final String CLAIMS_ROLES_KEY = "roles";
    private static final String CLAIMS_USER_ID_KEY = "sub";

    private final PublicKey authenticationPublicKey;

    /**
     * @param authenticationPublicKey the RSA public key of the Authentication service,
     *                               used to verify session token signatures
     */
    public ExternalJwtSessionAdapter(PublicKey authenticationPublicKey) {
        this.authenticationPublicKey = Objects.requireNonNull(
                authenticationPublicKey, "authenticationPublicKey must not be null");
    }

    /**
     * Parses and validates the session token's signature, expiration, and claims.
     *
     * @param sessionToken the raw JWT string
     * @return a {@link UserIdentityContext} extracted from the token's claims
     * @throws InvalidSessionTokenException if verification fails for any reason
     */
    @Override
    public UserIdentityContext validate(String sessionToken) {
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
