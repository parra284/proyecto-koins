package com.finapp.gateway.application.port;

import com.finapp.gateway.domain.valueobject.UserIdentityContext;

/**
 * Output Port — signs an internal application JWT (short-lived, 1 minute)
 * using the Gateway's private key, embedding the authenticated identity
 * for downstream microservices.
 *
 * <p>Implementations reside in the adapter layer and depend on specific
 * JWT signing libraries and the Gateway's RSA private key.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IApplicationTokenSigner {

    /**
     * Creates a signed JWT containing the user's identity and roles,
     * valid for a short window (typically 1 minute).
     *
     * @param identity the validated user identity to embed in the token claims
     * @return the signed JWT string ready to be injected into downstream headers
     */
    String sign(UserIdentityContext identity);
}
