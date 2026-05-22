package com.finapp.transactions.application.port.output;

import com.finapp.transactions.domain.entity.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — formal persistence contract for the {@code Transaction} aggregate.
 *
 * <p>This is a <em>pure Java interface</em> owned by the Application layer.
 * Infrastructure adapters (e.g. MariaDB) implement it on the other side
 * of the Dependency Inversion boundary.</p>
 *
 * <p><strong>Design note:</strong> There is no {@code delete} method.
 * Accounting audit requirements demand a full, immutable ledger.
 * Corrections are modeled via {@link com.finapp.transactions.domain.valueobject.TransactionType#REVERSAL}
 * compensating entries.</p>
 */
public interface TransactionRepository {

    /**
     * Persists a new transaction.
     *
     * @param transaction fully validated domain entity
     * @return the persisted transaction (with any generated fields populated)
     */
    Transaction save(Transaction transaction);

    /**
     * Retrieves a transaction by its unique identifier.
     *
     * @param id transaction UUID
     * @return the matching transaction, or empty if not found
     */
    Optional<Transaction> findById(UUID id);

    /**
     * Retrieves the complete transaction history for a given user,
     * ordered by date descending.
     *
     * @param userId the owner's identifier (extracted from JWT)
     * @return list of transactions belonging to the user
     */
    List<Transaction> findByUserId(String userId);
}
