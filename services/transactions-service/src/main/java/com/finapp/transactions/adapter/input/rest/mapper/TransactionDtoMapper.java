package com.finapp.transactions.adapter.input.rest.mapper;

import com.finapp.transactions.adapter.input.rest.dto.TransactionRequestDTO;
import com.finapp.transactions.adapter.input.rest.dto.TransactionResponseDTO;
import com.finapp.transactions.application.usecase.CreateTransaction;
import com.finapp.transactions.domain.entity.Transaction;
import com.finapp.transactions.domain.valueobject.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Data Mapper — translates between REST DTOs and application-layer objects.
 *
 * <p>Lives in the adapter layer because it knows about both the external
 * contract ({@link TransactionRequestDTO}) and the internal domain model.</p>
 */
@Component
public class TransactionDtoMapper {

    /**
     * Converts an inbound DTO into a {@link CreateTransaction.Command}.
     *
     * @param dto    the REST request payload
     * @param userId the authenticated user (injected from JWT context)
     * @return a clean command ready for the interactor
     * @throws IllegalArgumentException if the {@code type} string is invalid
     */
    public CreateTransaction.Command toCommand(TransactionRequestDTO dto, String userId) {
        TransactionType type = TransactionType.valueOf(dto.type().toUpperCase());
        return new CreateTransaction.Command(
                userId,
                dto.amount(),
                type,
                dto.category(),
                dto.description()
        );
    }

    /**
     * Converts a domain entity into a response DTO.
     */
    public TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.id(),
                transaction.userId(),
                transaction.amount(),
                transaction.type().name(),
                transaction.category(),
                transaction.date(),
                transaction.description()
        );
    }

    /**
     * Batch conversion for lists.
     */
    public List<TransactionResponseDTO> toResponseDTOList(List<Transaction> transactions) {
        return transactions.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
