package com.finapp.gateway.domain.valueobject;

import java.util.Objects;

/**
 * Value Object — defines the Token Bucket parameters for rate limiting.
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>{@code bucketCapacity} must be greater than zero.</li>
 *   <li>{@code refillRatePerSecond} must be greater than zero.</li>
 * </ol>
 *
 * <p>This class is <em>completely immutable</em> — there are no setters.</p>
 *
 * <p><strong>Framework-free:</strong> No Spring, Redis, or external annotations.</p>
 */
public final class RateLimitRule {

    private final int bucketCapacity;
    private final int refillRatePerSecond;

    /**
     * Canonical constructor — validates all business invariants.
     *
     * @param bucketCapacity      maximum number of tokens the bucket can hold
     * @param refillRatePerSecond number of tokens added per second
     * @throws NullPointerException     if either parameter is null
     * @throws IllegalArgumentException if either parameter is not greater than zero
     */
    public RateLimitRule(Integer bucketCapacity, Integer refillRatePerSecond) {
        Objects.requireNonNull(bucketCapacity, "bucketCapacity must not be null");
        Objects.requireNonNull(refillRatePerSecond, "refillRatePerSecond must not be null");

        if (bucketCapacity <= 0) {
            throw new IllegalArgumentException(
                    "bucketCapacity must be greater than zero, got: " + bucketCapacity);
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException(
                    "refillRatePerSecond must be greater than zero, got: " + refillRatePerSecond);
        }

        this.bucketCapacity = bucketCapacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public int bucketCapacity()      { return bucketCapacity; }
    public int refillRatePerSecond() { return refillRatePerSecond; }

    /* ────────────────────────────────────────────────────────
       Equality by value
       ──────────────────────────────────────────────────────── */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RateLimitRule that)) return false;
        return bucketCapacity == that.bucketCapacity
                && refillRatePerSecond == that.refillRatePerSecond;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketCapacity, refillRatePerSecond);
    }

    @Override
    public String toString() {
        return "RateLimitRule[capacity=%d, refillRate=%d/s]"
                .formatted(bucketCapacity, refillRatePerSecond);
    }
}
