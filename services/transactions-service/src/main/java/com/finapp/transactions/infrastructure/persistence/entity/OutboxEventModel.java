package com.finapp.transactions.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity — physical ORM mapping for the {@code outbox_events} table.
 *
 * <p>Part of the Transactional Outbox pattern. Each row represents a domain
 * event that has been recorded within the same ACID transaction as the
 * aggregate state change but has not yet been relayed to the message broker.</p>
 *
 * <p>The {@link com.finapp.transactions.infrastructure.messaging.OutboxWorker}
 * polls this table, publishes pending events to Kafka, and marks them as
 * {@code published = true} upon receiving the broker ACK.</p>
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventModel {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "published", nullable = false)
    private boolean published;

    /* ── Constructors ─────────────────────────────────────── */

    public OutboxEventModel() {
        // Required by JPA
    }

    /* ── Accessors ────────────────────────────────────────── */

    public UUID getEventId()                     { return eventId; }
    public void setEventId(UUID eventId)         { this.eventId = eventId; }

    public String getEventType()                       { return eventType; }
    public void setEventType(String eventType)         { this.eventType = eventType; }

    public Instant getOccurredOn()                     { return occurredOn; }
    public void setOccurredOn(Instant occurredOn)      { this.occurredOn = occurredOn; }

    public String getPayload()                         { return payload; }
    public void setPayload(String payload)             { this.payload = payload; }

    public boolean isPublished()                       { return published; }
    public void setPublished(boolean published)        { this.published = published; }
}
