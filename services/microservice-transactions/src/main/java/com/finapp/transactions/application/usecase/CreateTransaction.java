package com.finapp.transactions.application.usecase;

import com.finapp.transactions.application.port.output.EventPublisher;
import com.finapp.transactions.application.port.output.TransactionRepository;
import com.finapp.transactions.domain.entity.Transaction;
import com.finapp.transactions.domain.event.TransactionCreatedEvent;
import com.finapp.transactions.domain.valueobject.TransactionType;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Command Interactor — orchestrates the creation of a new financial transaction.
 *
 * <p>Receives clean, pre-validated input from the adapter layer, delegates
 * entity construction to the {@link Transaction} factory (which enforces all
 * business invariants), persists the result, and emits a domain event to the
 * outbox for downstream consumers.</p>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class CreateTransaction {

    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;

    public CreateTransaction(TransactionRepository transactionRepository,
                             EventPublisher eventPublisher) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    /**
     * Input command carrying the raw data for a new transaction.
     */
    public record Command(
            String userId,
            BigDecimal amount,
            TransactionType type,
            String category,
            String description
    ) {
        public Command {
            Objects.requireNonNull(userId, "userId is required");
            Objects.requireNonNull(amount, "amount is required");
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(category, "category is required");
        }
    }

    /**
     * Executes the transaction creation flow:
     * <ol>
     *   <li>Constructs the domain entity (invariants validated in constructor).</li>
     *   <li>Persists the entity via the repository port.</li>
     *   <li>Publishes a {@link TransactionCreatedEvent} via the event publisher port.</li>
     * </ol>
     *
     * @param command input data
     * @return the persisted, fully-validated transaction
     */
    public Transaction execute(Command command) {
        Transaction transaction = Transaction.create(
                command.userId(),
                command.amount(),
                command.type(),
                command.category(),
                command.description()
        );

        Transaction persisted = transactionRepository.save(transaction);

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
