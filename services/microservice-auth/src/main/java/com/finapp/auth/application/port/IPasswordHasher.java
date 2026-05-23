package com.finapp.auth.application.port;

/**
 * Output Port — abstracts cryptographic password hashing and verification.
 *
 * <p>Implementations reside in the infrastructure layer and wrap specific
 * hashing algorithms (BCrypt, Argon2, SCrypt) with CPU-optimal configuration.</p>
 *
 * <p><strong>Security contract:</strong> Plaintext passwords enter this port
 * only after being structurally validated by the
 * {@link com.finapp.auth.domain.valueobject.Credentials} Value Object.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IPasswordHasher {

    /**
     * Produces a one-way cryptographic hash of the given plaintext password.
     *
     * @param rawPassword the plaintext password to hash
     * @return the encoded hash string (algorithm-specific format)
     */
    String hash(String rawPassword);

    /**
     * Verifies a plaintext password against a previously produced hash.
     *
     * <p>The implementation must perform constant-time comparison to
     * prevent timing side-channel attacks.</p>
     *
     * @param rawPassword  the plaintext password to verify
     * @param encodedHash  the stored hash to compare against
     * @return {@code true} if the password matches the hash
     */
    boolean verify(String rawPassword, String encodedHash);
}
