package com.finapp.transactions.infrastructure.persistence.repository;

import com.finapp.transactions.infrastructure.persistence.entity.OutboxEventModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository — low-level database access for the
 * {@code outbox_events} table.
 *
 * <p>Used by the {@link com.finapp.transactions.infrastructure.messaging.OutboxWorker}
 * to poll for unpublished events and mark them as published after successful
 * delivery to Kafka.</p>
 */
@Repository
public interface JpaOutboxRepository extends JpaRepository<OutboxEventModel, UUID> {

    /**
     * Retrieves a batch of unpublished events ordered by creation time.
     *
     * @param limit maximum number of events to fetch in one poll cycle
     */
    @Query("SELECT o FROM OutboxEventModel o WHERE o.published = false ORDER BY o.occurredOn ASC LIMIT :limit")
    List<OutboxEventModel> findPendingEvents(@Param("limit") int limit);

    /**
     * Marks an event as published after successful Kafka delivery (ACK received).
     */
    @Modifying
    @Query("UPDATE OutboxEventModel o SET o.published = true WHERE o.eventId = :eventId")
    void markAsPublished(@Param("eventId") UUID eventId);

    /**
     * Deletes events that have been successfully published,
     * used for periodic cleanup.
     */
    @Modifying
    @Query("DELETE FROM OutboxEventModel o WHERE o.published = true")
    void deletePublishedEvents();
}
