package com.finapp.auth.domain.valueobject;

/**
 * Domain Enum — represents the authorization roles available in the
 * financial ecosystem.
 *
 * <p>Each role maps to a distinct privilege tier used by the API Gateway
 * and downstream microservices to enforce access control.</p>
 *
 * <p><strong>Framework-free:</strong> No Spring Security or external annotations.</p>
 */
public enum Role {

    /**
     * Standard end-user with access to personal financial operations
     * (transactions, analysis, profile management).
     */
    CUSTOMER,

    /**
     * Back-office operator with access to administrative dashboards,
     * user management, and compliance tooling.
     */
    OPERATOR,

    /**
     * System administrator with full access including key rotation,
     * infrastructure configuration, and audit trails.
     */
    ADMIN;

    /**
     * Parses a role from a case-insensitive string representation.
     *
     * @param value the string to parse
     * @return the matching {@link Role}
     * @throws IllegalArgumentException if no matching role is found
     * @throws NullPointerException     if {@code value} is null
     */
    public static Role fromString(String value) {
        if (value == null) {
            throw new NullPointerException("Role value must not be null");
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown role: '%s'. Allowed values: CUSTOMER, OPERATOR, ADMIN"
                            .formatted(value));
        }
    }
}
