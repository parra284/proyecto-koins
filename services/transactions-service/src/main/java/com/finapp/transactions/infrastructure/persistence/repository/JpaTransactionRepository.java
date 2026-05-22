package com.finapp.transactions.infrastructure.persistence.repository;

import com.finapp.transactions.infrastructure.persistence.entity.TransactionDbModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository — low-level database access for the
 * {@code transactions} table.
 *
 * <p>This interface is consumed exclusively by the adapter layer
 * ({@link com.finapp.transactions.adapter.output.persistence.MariaDbTransactionRepository}),
 * never by application or domain code.</p>
 */
@Repository
public interface JpaTransactionRepository extends JpaRepository<TransactionDbModel, UUID> {

    /**
     * Retrieves all transactions for a user, ordered by date descending (newest first).
     */
    List<TransactionDbModel> findByUserIdOrderByDateDesc(String userId);
}
