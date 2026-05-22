package com.finapp.transactions.domain.valueobject;

/**
 * Strict enumeration of permitted financial movement types.
 * Eliminates primitive obsession by constraining the domain vocabulary.
 *
 * <ul>
 *   <li>{@code INCOME}   — Money received (salary, sales, refunds).</li>
 *   <li>{@code EXPENSE}  — Money spent (purchases, fees, bills).</li>
 *   <li>{@code REVERSAL} — Compensating entry that cancels a previous transaction.</li>
 * </ul>
 */
public enum TransactionType {

    INCOME("Ingreso"),
    EXPENSE("Gasto"),
    REVERSAL("Contra-asiento");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Human-readable label for UI / logging purposes.
     */
    public String displayName() {
        return displayName;
    }
}
