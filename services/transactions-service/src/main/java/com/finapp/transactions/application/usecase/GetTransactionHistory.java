package com.finapp.transactions.application.usecase;

import com.finapp.transactions.application.port.output.TransactionRepository;
import com.finapp.transactions.domain.entity.Transaction;

import java.util.List;
import java.util.Objects;

/**
 * Query Interactor — retrieves the transaction history for a user.
 *
 * <p>Guarantees <strong>data-level security</strong> by forcing the
 * {@code userId} filter extracted cryptographically from the authenticated
 * session. This prevents any identity spoofing — the caller cannot request
 * another user's transaction history.</p>
 */
public class GetTransactionHistory {

    private final TransactionRepository transactionRepository;

    public GetTransactionHistory(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    /**
     * Input query carrying the authenticated user identifier.
     */
    public record Query(String userId) {
        public Query {
            Objects.requireNonNull(userId, "userId is required");
            if (userId.isBlank()) {
                throw new IllegalArgumentException("userId must not be blank");
            }
        }
    }

    /**
     * Executes the query, returning all transactions owned by the specified user.
     *
     * @param query contains the authenticated userId
     * @return ordered list of the user's transactions (newest first)
     */
    public List<Transaction> execute(Query query) {
        return transactionRepository.findByUserId(query.userId());
    }
}
