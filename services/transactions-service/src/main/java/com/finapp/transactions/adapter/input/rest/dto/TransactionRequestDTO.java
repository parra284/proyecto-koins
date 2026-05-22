package com.finapp.transactions.adapter.input.rest.dto;

import java.math.BigDecimal;

/**
 * Inbound DTO — defines the public API contract for creating a transaction.
 *
 * <p>This flat data structure isolates the domain entity from external
 * mutations and validates the shape of the incoming JSON payload.</p>
 */
public record TransactionRequestDTO(
        BigDecimal amount,
        String type,
        String category,
        String description
) {}
