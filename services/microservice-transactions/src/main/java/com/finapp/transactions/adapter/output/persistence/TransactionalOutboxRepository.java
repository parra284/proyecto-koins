package com.finapp.transactions.adapter.output.persistence;

import com.finapp.transactions.application.port.output.EventPublisher;
import com.finapp.transactions.domain.event.DomainEvent;
import com.finapp.transactions.infrastructure.persistence.entity.OutboxEventModel;
import com.finapp.transactions.infrastructure.persistence.repository.JpaOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * Transactional Outbox — implements the {@link EventPublisher} port.
 *
 * <p>Instead of publishing events directly to Kafka (which would break
 * atomicity with the database write), this adapter serializes the domain
 * event into a JSON payload and stores it in the {@code outbox_events} table
 * within the <em>same ACID transaction</em> as the aggregate state change.</p>
 *
 * <p>A separate {@link com.finapp.transactions.infrastructure.messaging.OutboxWorker}
 * background process polls this table and relays pending events to Kafka,
 * guaranteeing at-least-once delivery and eventual consistency.</p>
 */
@Repository
public class TransactionalOutboxRepository implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionalOutboxRepository.class);

    private final JpaOutboxRepository jpaOutboxRepository;
    private final ObjectMapper objectMapper;

    public TransactionalOutboxRepository(JpaOutboxRepository jpaOutboxRepository,
                                         ObjectMapper objectMapper) {
        this.jpaOutboxRepository = jpaOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        OutboxEventModel outboxEntry = new OutboxEventModel();
        outboxEntry.setEventId(event.eventId());
        outboxEntry.setEventType(event.eventType());
        outboxEntry.setOccurredOn(event.occurredOn());
        outboxEntry.setPayload(serializePayload(event));
        outboxEntry.setPublished(false);

        jpaOutboxRepository.save(outboxEntry);

        log.debug("Domain event {} ({}) written to outbox", event.eventId(), event.eventType());
    }

    private String serializePayload(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.eventId(), e);
        }
    }
}
