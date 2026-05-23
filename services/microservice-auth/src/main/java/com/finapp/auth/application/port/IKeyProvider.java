package com.finapp.auth.application.port;

/**
 * Output Port — provides access to the service's cryptographic key material.
 *
 * <p>Implementations reside in the infrastructure layer and may load keys
 * from a secrets vault, environment variables, or dynamic key generators.</p>
 *
 * <p>Used by:</p>
 * <ul>
 *   <li>The JWKS endpoint controller — to expose the public key for
 *       Zero-Trust token validation by the API Gateway.</li>
 *   <li>The session token signer adapter — to access the private key
 *       for JWT signing.</li>
 * </ul>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IKeyProvider {

    /**
     * Returns the X.509-encoded bytes of the active RSA public key.
     *
     * @return the public key bytes in X.509/DER format
     */
    byte[] publicKeyEncoded();

    /**
     * Returns the PKCS8-encoded bytes of the active RSA private key.
     *
     * @return the private key bytes in PKCS8/DER format
     */
    byte[] privateKeyEncoded();

    /**
     * Returns the Key ID (kid) of the currently active key pair,
     * used in JWKS and JWT headers for key rotation tracking.
     *
     * @return the active key identifier string
     */
    String activeKeyId();

    /**
     * Returns the algorithm identifier (e.g., "RS256") used for
     * token signing.
     *
     * @return the algorithm name
     */
    String algorithm();
}
