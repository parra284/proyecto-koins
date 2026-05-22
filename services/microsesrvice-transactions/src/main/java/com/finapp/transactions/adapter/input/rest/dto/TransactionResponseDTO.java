package com.finapp.transactions.adapter.input.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound DTO — defines the public API response contract.
 *
 * <p>Contains only the fields the frontend/consumer needs.
 * Internal domain details are never leaked.</p>
 */
public record TransactionResponseDTO(
        UUID id,
        String userId,
        BigDecimal amount,
        String type,
        String category,
        LocalDateTime date,
        String description
) {}
