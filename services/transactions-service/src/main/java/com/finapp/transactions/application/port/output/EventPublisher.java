package com.finapp.transactions.application.port.output;

import com.finapp.transactions.domain.event.DomainEvent;

/**
 * Output port — reactive decoupling contract for domain event publishing.
 *
 * <p>The application core emits events through this interface without any
 * knowledge of the underlying messaging infrastructure (Kafka, RabbitMQ, etc.).</p>
 *
 * <p>The infrastructure adapter ({@code TransactionalOutboxRepository}) implements
 * this port by writing events to a transactional outbox table, guaranteeing
 * at-least-once delivery through the Transactional Outbox pattern.</p>
 */
public interface EventPublisher {

    /**
     * Publishes a domain event to be eventually delivered to external consumers.
     *
     * <p>Implementations must guarantee that the event is durably recorded
     * within the same ACID transaction as the aggregate state change.</p>
     *
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);
}
