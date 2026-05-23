package com.finapp.auth.infrastructure.cache;

import com.finapp.auth.application.port.IDenylistRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Infrastructure Adapter — implements {@link IDenylistRepository} using
 * Redis for high-speed token revocation lookups.
 *
 * <p>Revoked token JTIs are stored with a TTL matching the token's
 * remaining validity, ensuring automatic cleanup without manual
 * eviction.</p>
 *
 * <p>This adapter also serves as a high-speed proxy for failed login
 * rate counting by IP/identity, providing a first line of defense
 * against DoS attacks <em>before</em> hitting the relational database.</p>
 */
public class FailedLoginCacheAdapter implements IDenylistRepository {

    private static final String DENYLIST_PREFIX = "auth:denylist:";
    private static final String FAILED_LOGIN_PREFIX = "auth:failed_login:";

    private final StringRedisTemplate redisTemplate;

    public FailedLoginCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(
                redisTemplate, "redisTemplate must not be null");
    }

    /* ────────────────────────────────────────────────────────
       IDenylistRepository — Token Revocation
       ──────────────────────────────────────────────────────── */

    @Override
    public void revoke(String tokenId, long ttlSeconds) {
        Objects.requireNonNull(tokenId, "tokenId must not be null");
        String key = DENYLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "REVOKED", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        Objects.requireNonNull(tokenId, "tokenId must not be null");
        String key = DENYLIST_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /* ────────────────────────────────────────────────────────
       Failed Login Rate Limiting (DoS Mitigation)
       ──────────────────────────────────────────────────────── */

    /**
     * Records a failed login attempt for the given identity key
     * (IP address or email) with a sliding TTL window.
     *
     * @param identityKey  the IP address or email to track
     * @param windowSeconds the TTL window for counting attempts
     * @return the current count of failed attempts within the window
     */
    public long recordFailedAttempt(String identityKey, long windowSeconds) {
        String key = FAILED_LOGIN_PREFIX + identityKey;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count != null ? count : 0L;
    }

    /**
     * Returns the current count of failed login attempts for the given
     * identity key within the active window.
     *
     * @param identityKey the IP address or email to check
     * @return the current failure count (0 if no entries exist)
     */
    public long getFailedAttemptCount(String identityKey) {
        String key = FAILED_LOGIN_PREFIX + identityKey;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * Checks whether the given identity has exceeded the maximum
     * allowed failed login rate.
     *
     * @param identityKey the IP address or email to check
     * @param maxAttempts the maximum allowed attempts in the window
     * @return {@code true} if the identity should be rate-limited
     */
    public boolean isRateLimited(String identityKey, int maxAttempts) {
        return getFailedAttemptCount(identityKey) >= maxAttempts;
    }

    /**
     * Clears the failed login counter for the given identity
     * (called after a successful login).
     *
     * @param identityKey the IP address or email to clear
     */
    public void clearFailedAttempts(String identityKey) {
        String key = FAILED_LOGIN_PREFIX + identityKey;
        redisTemplate.delete(key);
    }
}
