package com.finapp.auth.domain.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when a password does not meet the banking-grade complexity
 * requirements enforced by the {@link com.finapp.auth.domain.valueobject.Credentials}
 * Value Object.
 *
 * <p>Carries the full list of policy violations so that clients can
 * display actionable feedback without multiple round-trips.</p>
 */
public final class WeakPasswordException extends AuthDomainException {

    private final List<String> violations;

    /**
     * @param violations the list of specific complexity rules that were violated
     */
    public WeakPasswordException(List<String> violations) {
        super("Password does not meet complexity requirements: " + violations);
        this.violations = Collections.unmodifiableList(violations);
    }

    /**
     * Returns the list of individual policy violations.
     */
    public List<String> violations() { return violations; }
}
