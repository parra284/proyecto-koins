package com.finapp.transactions.application.usecase;

import com.finapp.transactions.application.port.output.EventPublisher;
import com.finapp.transactions.application.port.output.TransactionRepository;
import com.finapp.transactions.domain.entity.Transaction;
import com.finapp.transactions.domain.event.TransactionCreatedEvent;

import java.util.Objects;
import java.util.UUID;

/**
 * Command Interactor — executes the annulment of a financial movement.
 *
 * <p>Instead of deleting or mutating the original transaction (which would
 * violate accounting audit requirements), this interactor creates a
 * {@link com.finapp.transactions.domain.valueobject.TransactionType#REVERSAL}
 * compensating entry with the same absolute amount.</p>
 *
 * <p>Security: validates that the requesting user owns the original transaction
 * before allowing the reversal (data-level security).</p>
 */
public class ReverseTransaction {

    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;

    public ReverseTransaction(TransactionRepository transactionRepository,
                              EventPublisher eventPublisher) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    /**
     * Input command for reversing a transaction.
     *
     * @param originalTransactionId the UUID of the transaction to reverse
     * @param requestingUserId      the authenticated user requesting the reversal
     */
    public record Command(
            UUID originalTransactionId,
            String requestingUserId
    ) {
        public Command {
            Objects.requireNonNull(originalTransactionId, "originalTransactionId is required");
            Objects.requireNonNull(requestingUserId, "requestingUserId is required");
        }
    }

    /**
     * Executes the reversal flow:
     * <ol>
     *   <li>Fetches the original transaction by ID.</li>
     *   <li>Validates ownership (the requesting user must match the original owner).</li>
     *   <li>Creates a REVERSAL compensating entry via the domain entity.</li>
     *   <li>Persists the reversal transaction.</li>
     *   <li>Publishes a domain event for downstream consumers.</li>
     * </ol>
     *
     * @param command input data
     * @return the newly created reversal transaction
     * @throws IllegalArgumentException if the original transaction is not found
     *                                  or the user is not the owner
     */
    public Transaction execute(Command command) {
        Transaction original = transactionRepository.findById(command.originalTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found: " + command.originalTransactionId()));

        Transaction reversal = original.createReversal(command.requestingUserId());

        Transaction persisted = transactionRepository.save(reversal);

        TransactionCreatedEvent event = new TransactionCreatedEvent(
                persisted.id(),
                persisted.userId(),
                persisted.amount(),
                persisted.type(),
                persisted.category(),
                persisted.date(),
                persisted.description()
        );

        eventPublisher.publish(event);

        return persisted;
    }
}
