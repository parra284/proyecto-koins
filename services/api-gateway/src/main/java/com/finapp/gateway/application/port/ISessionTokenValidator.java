package com.finapp.gateway.application.port;

import com.finapp.gateway.domain.valueobject.UserIdentityContext;

/**
 * Output Port — validates an incoming session token (issued by the
 * Authentication service) and extracts the user's identity context.
 *
 * <p>Implementations reside in the adapter layer and depend on specific
 * JWT libraries and the Authentication service's public key.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface ISessionTokenValidator {

    /**
     * Validates the cryptographic signature, expiration, and claims of
     * the given session token.
     *
     * @param sessionToken the raw JWT string from the client's Authorization header
     * @return a {@link UserIdentityContext} extracted from the validated token claims
     * @throws com.finapp.gateway.domain.exception.InvalidSessionTokenException
     *         if the token is expired, malformed, or fails signature verification
     */
    UserIdentityContext validate(String sessionToken);
}
