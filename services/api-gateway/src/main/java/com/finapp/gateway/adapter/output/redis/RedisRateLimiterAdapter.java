package com.finapp.gateway.adapter.output.redis;

import com.finapp.gateway.application.port.IRateLimiterRepository;
import com.finapp.gateway.domain.valueobject.RateLimitRule;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Adapter — implements the Token Bucket rate-limiting algorithm using
 * atomic Redis operations via a Lua script.
 *
 * <p>
 * Implements {@link IRateLimiterRepository}. The Lua script guarantees
 * atomicity of the refill-check-consume cycle, preventing race conditions
 * under concurrent access.
 * </p>
 *
 * <p>
 * Each client gets a Redis key {@code rate_limit:{clientId}} storing
 * the current token count and a companion timestamp key for the last
 * refill time.
 * </p>
 */
public class RedisRateLimiterAdapter implements IRateLimiterRepository {

    private static final String KEY_PREFIX = "rate_limit:";

    /**
     * Lua script implementing the Token Bucket algorithm atomically.
     *
     * <p>
     * KEYS[1] = bucket key (stores current tokens and last-refill timestamp).
     * </p>
     * <p>
     * ARGV[1] = bucket capacity, ARGV[2] = refill rate/sec, ARGV[3] = current time
     * millis.
     * </p>
     *
     * <p>
     * Returns {@code 1} if a token was consumed, {@code 0} if the bucket is empty.
     * </p>
     */
    private static final String TOKEN_BUCKET_LUA = """
            local bucket_key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            local bucket = redis.call('HMGET', bucket_key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])

            if tokens == nil then
                tokens = capacity
                last_refill = now
            end

            local elapsed = math.max(0, (now - last_refill) / 1000.0)
            local refill = math.floor(elapsed * refill_rate)

            if refill > 0 then
                tokens = math.min(capacity, tokens + refill)
                last_refill = now
            end

            local allowed = 0
            if tokens > 0 then
                tokens = tokens - 1
                allowed = 1
            end

            redis.call('HMSET', bucket_key, 'tokens', tokens, 'last_refill', last_refill)
            redis.call('EXPIRE', bucket_key, math.ceil(capacity / refill_rate) + 10)

            return allowed
            """;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    public RedisRateLimiterAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(
                redisTemplate, "redisTemplate must not be null");
        this.tokenBucketScript = RedisScript.of(TOKEN_BUCKET_LUA, Long.class);
    }

    /**
     * Atomically attempts to consume one token from the client's bucket.
     *
     * <p>
     * This method blocks briefly to bridge the reactive Redis call
     * into the synchronous port interface contract. The Lua script
     * executes entirely on the Redis server, ensuring atomicity.
     * </p>
     *
     * @param clientId the unique client identifier
     * @param rule     the rate-limit configuration
     * @return {@code true} if a token was consumed, {@code false} otherwise
     */
    @Override
    public boolean tryConsume(String clientId, RateLimitRule rule) {
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(rule, "rule must not be null");

        String key = KEY_PREFIX + clientId;
        long nowMillis = System.currentTimeMillis();

        List<String> keys = Collections.singletonList(key);
        List<String> args = List.of(
                String.valueOf(rule.bucketCapacity()),
                String.valueOf(rule.refillRatePerSecond()),
                String.valueOf(nowMillis)
        );

        Long result = redisTemplate.execute(tokenBucketScript, keys, args)
                .single()
                .onErrorResume(throwable -> Mono.just(1L)) // fail-open on Redis errors
                .block();

        return result != null && result == 1L;
    }
}
