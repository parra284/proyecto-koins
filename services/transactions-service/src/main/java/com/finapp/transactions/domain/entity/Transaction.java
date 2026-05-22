package com.finapp.transactions.domain.entity;

import com.finapp.transactions.domain.valueobject.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Core Domain Entity — represents an immutable financial movement.
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>{@code amount} must be strictly greater than zero.</li>
 *   <li>{@code date} cannot be in the future.</li>
 *   <li>{@code userId} and {@code category} must be non-null and non-blank.</li>
 * </ol>
 *
 * <p>This class is <em>completely immutable</em> — there are no setters.
 * Corrections are performed via a compensating {@link TransactionType#REVERSAL} entry,
 * preserving the full audit trail required by accounting regulations.</p>
 *
 * <p><strong>Framework-free:</strong> No Spring, JPA, or Hibernate annotations.</p>
 */
public final class Transaction {

    private final UUID id;
    private final String userId;
    private final BigDecimal amount;
    private final TransactionType type;
    private final String category;
    private final LocalDateTime date;
    private final String description;

    /**
     * Canonical constructor — validates all business invariants.
     *
     * @throws IllegalArgumentException if any invariant is violated
     */
    public Transaction(UUID id,
                       String userId,
                       BigDecimal amount,
                       TransactionType type,
                       String category,
                       LocalDateTime date,
                       String description) {

        Objects.requireNonNull(id, "Transaction id must not be null");
        requireNonBlank(userId, "userId");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(type, "type must not be null");
        requireNonBlank(category, "category");
        Objects.requireNonNull(date, "date must not be null");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be strictly greater than zero, got: " + amount);
        }
        if (date.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("date cannot be in the future, got: " + date);
        }

        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.description = description == null ? "" : description;
    }

    /* ────────────────────────────────────────────────────────
       Factory method for creating new transactions
       ──────────────────────────────────────────────────────── */

    /**
     * Factory that generates a fresh UUID and timestamps the transaction to <em>now</em>.
     */
    public static Transaction create(String userId,
                                     BigDecimal amount,
                                     TransactionType type,
                                     String category,
                                     String description) {
        return new Transaction(
                UUID.randomUUID(),
                userId,
                amount,
                type,
                category,
                LocalDateTime.now(),
                description
        );
    }

    /**
     * Creates a {@link TransactionType#REVERSAL} that compensates this transaction.
     * The reversal carries the same absolute amount and category for traceability.
     *
     * @param requestingUserId the user requesting the reversal — must match the original owner
     * @throws IllegalArgumentException if the requesting user is not the owner
     */
    public Transaction createReversal(String requestingUserId) {
        if (!this.userId.equals(requestingUserId)) {
            throw new IllegalArgumentException(
                    "User '" + requestingUserId + "' is not the owner of transaction " + this.id);
        }
        return new Transaction(
                UUID.randomUUID(),
                this.userId,
                this.amount,
                TransactionType.REVERSAL,
                this.category,
                LocalDateTime.now(),
                "REVERSAL of transaction " + this.id
        );
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public UUID id()              { return id; }
    public String userId()        { return userId; }
    public BigDecimal amount()    { return amount; }
    public TransactionType type() { return type; }
    public String category()      { return category; }
    public LocalDateTime date()   { return date; }
    public String description()   { return description; }

    /* ────────────────────────────────────────────────────────
       Equality by identity (UUID)
       ──────────────────────────────────────────────────────── */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Transaction[id=%s, userId=%s, amount=%s, type=%s, category=%s, date=%s]"
                .formatted(id, userId, amount, type, category, date);
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }
}
