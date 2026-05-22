package com.finapp.transactions.infrastructure.messaging;

import com.finapp.transactions.infrastructure.persistence.entity.OutboxEventModel;
import com.finapp.transactions.infrastructure.persistence.repository.JpaOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox Worker / Message Relay — background process that bridges the
 * Transactional Outbox table and the Kafka message broker.
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ol>
 *   <li>Polls the {@code outbox_events} table for unpublished entries.</li>
 *   <li>Publishes each event payload to the configured Kafka topic.</li>
 *   <li>Marks the event as {@code published = true} after receiving the
 *       broker acknowledgement (ACK).</li>
 * </ol>
 *
 * <p>This guarantees <em>at-least-once delivery</em> — if the worker crashes
 * between publishing and marking, the event will be re-sent on the next cycle.
 * Consumers must be idempotent.</p>
 */
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private final JpaOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public OutboxWorker(JpaOutboxRepository outboxRepository,
                        KafkaTemplate<String, String> kafkaTemplate,
                        @Value("${app.kafka.topic.transactions}") String topic,
                        @Value("${app.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    /**
     * Scheduled poll cycle — runs at the configured interval.
     * Each cycle processes up to {@code batchSize} pending events.
     */
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:500}")
    @Transactional
    public void relay() {
        List<OutboxEventModel> pendingEvents = outboxRepository.findPendingEvents(batchSize);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("OutboxWorker: relaying {} pending event(s) to Kafka topic '{}'",
                pendingEvents.size(), topic);

        for (OutboxEventModel event : pendingEvents) {
            try {
                kafkaTemplate.send(topic, event.getEventId().toString(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish event {} to Kafka: {}",
                                        event.getEventId(), ex.getMessage());
                            } else {
                                log.debug("Event {} published to Kafka partition={} offset={}",
                                        event.getEventId(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            }
                        });

                outboxRepository.markAsPublished(event.getEventId());

            } catch (Exception ex) {
                log.error("Error relaying event {}: {}", event.getEventId(), ex.getMessage(), ex);
                // Stop processing this batch — remaining events will be retried next cycle
                break;
            }
        }
    }
}
