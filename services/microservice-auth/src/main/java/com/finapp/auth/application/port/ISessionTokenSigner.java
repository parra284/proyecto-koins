package com.finapp.auth.application.port;

/**
 * Output Port — signs external session tokens (JWT) using the Auth
 * service's RSA private key.
 *
 * <p>Implementations reside in the infrastructure layer and depend on
 * specific JWT libraries and the service's asymmetric key pair.</p>
 *
 * <p>The signed token is the external credential that clients present
 * to the API Gateway, which validates it using the corresponding
 * public key exposed via the JWKS endpoint.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface ISessionTokenSigner {

    /**
     * Signs a new session JWT embedding the user's identity and role.
     *
     * @param userId the user's opaque UUID string
     * @param email  the user's email address
     * @param role   the user's authorization role name
     * @return the signed JWT string
     */
    String sign(String userId, String email, String role);
}
