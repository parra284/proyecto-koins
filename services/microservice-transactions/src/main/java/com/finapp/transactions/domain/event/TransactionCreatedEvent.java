package com.finapp.transactions.domain.event;

import com.finapp.transactions.domain.valueobject.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event raised whenever a new {@code Transaction} is successfully persisted.
 *
 * <p>Carries the full snapshot of the transaction so that downstream consumers
 * (e.g. Microservice Analysis) can process it without calling back.</p>
 *
 * <p><strong>Immutable record — pure Java, no framework annotations.</strong></p>
 */
public final class TransactionCreatedEvent extends DomainEvent {

    public static final String TYPE = "TRANSACTION_CREATED";

    private final UUID transactionId;
    private final String userId;
    private final BigDecimal amount;
    private final TransactionType transactionType;
    private final String category;
    private final LocalDateTime transactionDate;
    private final String description;

    public TransactionCreatedEvent(UUID transactionId,
                                   String userId,
                                   BigDecimal amount,
                                   TransactionType transactionType,
                                   String category,
                                   LocalDateTime transactionDate,
                                   String description) {
        super();
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.category = category;
        this.transactionDate = transactionDate;
        this.description = description;
    }

    public TransactionCreatedEvent(UUID eventId,
                                   Instant occurredOn,
                                   UUID transactionId,
                                   String userId,
                                   BigDecimal amount,
                                   TransactionType transactionType,
                                   String category,
                                   LocalDateTime transactionDate,
                                   String description) {
        super(eventId, occurredOn);
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.category = category;
        this.transactionDate = transactionDate;
        this.description = description;
    }

    @Override
    public String eventType() {
        return TYPE;
    }

    public UUID transactionId()             { return transactionId; }
    public String userId()                  { return userId; }
    public BigDecimal amount()              { return amount; }
    public TransactionType transactionType() { return transactionType; }
    public String category()                { return category; }
    public LocalDateTime transactionDate()  { return transactionDate; }
    public String description()             { return description; }

    @Override
    public String toString() {
        return "TransactionCreatedEvent[eventId=%s, transactionId=%s, userId=%s, amount=%s, type=%s]"
                .formatted(eventId(), transactionId, userId, amount, transactionType);
    }
}
