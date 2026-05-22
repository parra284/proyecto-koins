package com.finapp.transactions.adapter.output.persistence;

import com.finapp.transactions.application.port.output.TransactionRepository;
import com.finapp.transactions.domain.entity.Transaction;
import com.finapp.transactions.domain.valueobject.TransactionType;
import com.finapp.transactions.infrastructure.persistence.entity.TransactionDbModel;
import com.finapp.transactions.infrastructure.persistence.repository.JpaTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure adapter implementing the {@link TransactionRepository} port.
 *
 * <p>Translates between the pure domain entity {@link Transaction} and the
 * JPA-annotated {@link TransactionDbModel}, keeping the domain layer
 * completely free of persistence concerns.</p>
 */
@Repository
public class MariaDbTransactionRepository implements TransactionRepository {

    private final JpaTransactionRepository jpaRepository;

    public MariaDbTransactionRepository(JpaTransactionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionDbModel model = toDbModel(transaction);
        TransactionDbModel saved = jpaRepository.save(model);
        return toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Transaction> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /* ────────────────────────────────────────────────────────
       Mapping: Domain ↔ Persistence Model
       ──────────────────────────────────────────────────────── */

    private TransactionDbModel toDbModel(Transaction tx) {
        TransactionDbModel model = new TransactionDbModel();
        model.setId(tx.id());
        model.setUserId(tx.userId());
        model.setAmount(tx.amount());
        model.setType(tx.type().name());
        model.setCategory(tx.category());
        model.setDate(tx.date());
        model.setDescription(tx.description());
        return model;
    }

    private Transaction toDomain(TransactionDbModel model) {
        return new Transaction(
                model.getId(),
                model.getUserId(),
                model.getAmount(),
                TransactionType.valueOf(model.getType()),
                model.getCategory(),
                model.getDate(),
                model.getDescription()
        );
    }
}
