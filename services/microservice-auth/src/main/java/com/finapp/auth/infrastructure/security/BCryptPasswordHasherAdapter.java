package com.finapp.auth.infrastructure.security;

import com.finapp.auth.application.port.IPasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Infrastructure Adapter — implements {@link IPasswordHasher} wrapping
 * Spring Security's {@link BCryptPasswordEncoder} with an optimal
 * cost factor for banking-grade security.
 *
 * <p>BCrypt with a strength of <strong>12</strong> provides approximately
 * 2^12 = 4,096 iterations, balancing CPU cost against brute-force
 * resistance. The encoder internally generates a random salt per hash
 * and performs constant-time comparison during verification.</p>
 */
public class BCryptPasswordHasherAdapter implements IPasswordHasher {

    /**
     * BCrypt cost factor (log2 rounds). Strength 12 is recommended for
     * financial systems — strong enough to resist GPU attacks while keeping
     * login latency under 300ms on modern hardware.
     */
    private static final int BCRYPT_STRENGTH = 12;

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasherAdapter() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Hashes the plaintext password using BCrypt with a random salt.
     *
     * @param rawPassword the plaintext password (already validated by Credentials VO)
     * @return the BCrypt hash string in the format {@code $2a$12$...}
     */
    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * Verifies a plaintext password against a BCrypt hash using
     * constant-time comparison to prevent timing side-channel attacks.
     *
     * @param rawPassword the plaintext password to verify
     * @param encodedHash the stored BCrypt hash
     * @return {@code true} if the password matches
     */
    @Override
    public boolean verify(String rawPassword, String encodedHash) {
        return encoder.matches(rawPassword, encodedHash);
    }
}
