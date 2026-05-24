package com.finapp.transactions.adapter.output.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finapp.transactions.domain.event.TransactionCreatedEvent;
import com.finapp.transactions.domain.valueobject.TransactionType;
import com.finapp.transactions.infrastructure.persistence.entity.OutboxEventModel;
import com.finapp.transactions.infrastructure.persistence.repository.JpaOutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionalOutboxRepositoryTest {

    private final JpaOutboxRepository jpaOutboxRepository = mock(JpaOutboxRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final TransactionalOutboxRepository repository =
            new TransactionalOutboxRepository(jpaOutboxRepository, objectMapper);

    @Test
    void publishSerializesTransactionCreatedEventPayload() throws Exception {
        UUID transactionId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId,
                "user-123",
                new BigDecimal("42.50"),
                TransactionType.INCOME,
                "salary",
                LocalDateTime.of(2026, 5, 23, 20, 7, 34),
                "monthly payroll"
        );

        repository.publish(event);

        ArgumentCaptor<OutboxEventModel> captor = ArgumentCaptor.forClass(OutboxEventModel.class);
        verify(jpaOutboxRepository).save(captor.capture());

        OutboxEventModel saved = captor.getValue();
        JsonNode payload = objectMapper.readTree(saved.getPayload());

        assertThat(saved.getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getEventType()).isEqualTo(TransactionCreatedEvent.TYPE);
        assertThat(saved.isPublished()).isFalse();
        assertThat(payload.get("eventId").asText()).isEqualTo(event.eventId().toString());
        assertThat(payload.get("eventType").asText()).isEqualTo(TransactionCreatedEvent.TYPE);
        assertThat(payload.get("transactionId").asText()).isEqualTo(transactionId.toString());
        assertThat(payload.get("userId").asText()).isEqualTo("user-123");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("42.50");
        assertThat(payload.get("transactionType").asText()).isEqualTo("INCOME");
        assertThat(payload.get("category").asText()).isEqualTo("salary");
        assertThat(payload.get("description").asText()).isEqualTo("monthly payroll");
    }
}
