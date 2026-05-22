package com.finapp.transactions.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events emitted by the Transactions bounded context.
 *
 * <p>Every event is uniquely identified ({@code eventId}) and timestamped to
 * support idempotency checks on the consumer side (Microservice Analysis).</p>
 *
 * <p><strong>Framework-free:</strong> No Spring or messaging annotations.</p>
 */
public abstract sealed class DomainEvent permits TransactionCreatedEvent {

    private final UUID eventId;
    private final Instant occurredOn;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
    }

    protected DomainEvent(UUID eventId, Instant occurredOn) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
    }

    /** Globally unique event identifier — used for consumer-side idempotency. */
    public UUID eventId()      { return eventId; }

    /** Moment at which the domain event occurred. */
    public Instant occurredOn() { return occurredOn; }

    /** Logical event type name used for routing / deserialization on the consumer side. */
    public abstract String eventType();
}
