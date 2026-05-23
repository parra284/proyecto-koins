package com.finapp.auth.domain.valueobject;

import com.finapp.auth.domain.exception.InvalidEmailException;
import com.finapp.auth.domain.exception.WeakPasswordException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object — encapsulates and structurally validates raw user credentials
 * (email + plaintext password) before they enter any business workflow.
 *
 * <p><strong>Security contract:</strong> Plaintext passwords never travel through
 * the domain or application layers without first being encapsulated and validated
 * inside this Value Object. The actual cryptographic hashing is delegated to the
 * infrastructure layer via the {@code IPasswordHasher} port.</p>
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>Email must conform to a subset of RFC 5322 syntax.</li>
 *   <li>Password must satisfy banking-grade complexity requirements:
 *       minimum 12 characters, at least one uppercase letter, one lowercase letter,
 *       one digit, one special character, and no whitespace.</li>
 * </ol>
 *
 * <p><strong>Framework-free:</strong> No Spring, BCrypt, or external annotations.</p>
 */
public final class Credentials {

    /* ────────────────────────────────────────────────────────
       RFC 5322 simplified email pattern
       ──────────────────────────────────────────────────────── */

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
                    + "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
                    + "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );

    /* ────────────────────────────────────────────────────────
       Banking-grade password complexity rules
       ──────────────────────────────────────────────────────── */

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    private final String email;
    private final String rawPassword;

    /**
     * Canonical constructor — validates email syntax (RFC 5322) and password
     * complexity (banking-grade) at construction time.
     *
     * @param email       the user's email address
     * @param rawPassword the user's plaintext password
     * @throws InvalidEmailException if the email fails RFC 5322 validation
     * @throws WeakPasswordException if the password fails complexity checks
     */
    public Credentials(String email, String rawPassword) {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");

        validateEmail(email);
        validatePasswordComplexity(rawPassword);

        this.email = email.trim().toLowerCase();
        this.rawPassword = rawPassword;
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public String email()       { return email; }
    public String rawPassword() { return rawPassword; }

    /* ────────────────────────────────────────────────────────
       Email validation — RFC 5322 subset
       ──────────────────────────────────────────────────────── */

    private static void validateEmail(String email) {
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidEmailException("Email must not be empty");
        }
        if (trimmed.length() > 254) {
            throw new InvalidEmailException(
                    "Email exceeds maximum length of 254 characters");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidEmailException(
                    "Email does not conform to RFC 5322 syntax: " + trimmed);
        }
    }

    /* ────────────────────────────────────────────────────────
       Password complexity validation — Banking grade
       ──────────────────────────────────────────────────────── */

    private static void validatePasswordComplexity(String password) {
        List<String> violations = new ArrayList<>();

        if (password.length() < MIN_PASSWORD_LENGTH) {
            violations.add("must be at least %d characters (got %d)"
                    .formatted(MIN_PASSWORD_LENGTH, password.length()));
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            violations.add("must not exceed %d characters (got %d)"
                    .formatted(MAX_PASSWORD_LENGTH, password.length()));
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            violations.add("must contain at least one uppercase letter [A-Z]");
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            violations.add("must contain at least one lowercase letter [a-z]");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            violations.add("must contain at least one digit [0-9]");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            violations.add("must contain at least one special character");
        }
        if (WHITESPACE_PATTERN.matcher(password).find()) {
            violations.add("must not contain whitespace characters");
        }

        if (!violations.isEmpty()) {
            throw new WeakPasswordException(
                    Collections.unmodifiableList(violations));
        }
    }

    /* ────────────────────────────────────────────────────────
       Equality by value (email only — password excluded
       for security)
       ──────────────────────────────────────────────────────── */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Credentials that)) return false;
        return email.equals(that.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    /**
     * Never includes the raw password in string representations.
     */
    @Override
    public String toString() {
        return "Credentials[email=%s]".formatted(email);
    }
}
