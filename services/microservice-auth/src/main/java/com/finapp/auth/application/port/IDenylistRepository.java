package com.finapp.auth.application.port;

/**
 * Output Port — manages a high-speed denylist of revoked session tokens.
 *
 * <p>Implementations reside in the infrastructure layer, typically backed
 * by Redis with automatic TTL-based expiration aligned to the token's
 * original expiry.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IDenylistRepository {

    /**
     * Adds a token identifier to the denylist, effectively revoking it.
     *
     * @param tokenId       the unique identifier (JTI) of the revoked token
     * @param ttlSeconds    the number of seconds until the entry auto-expires
     *                      (should match the token's remaining validity)
     */
    void revoke(String tokenId, long ttlSeconds);

    /**
     * Checks whether a token identifier has been revoked.
     *
     * @param tokenId the unique identifier (JTI) to check
     * @return {@code true} if the token has been revoked and the entry
     *         has not yet expired
     */
    boolean isRevoked(String tokenId);
}
