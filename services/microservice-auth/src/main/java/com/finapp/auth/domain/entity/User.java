package com.finapp.auth.domain.entity;

import com.finapp.auth.domain.valueobject.AccountSecurityState;
import com.finapp.auth.domain.valueobject.Role;

import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root — represents an authenticated identity within the
 * financial ecosystem.
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>{@code userId} must be a non-null UUID v4 (opaque identifier).</li>
 *   <li>{@code email} must be non-null and non-blank (structural validation
 *       is performed by {@link com.finapp.auth.domain.valueobject.Credentials}).</li>
 *   <li>{@code passwordHash} must be non-null and non-blank (the raw password
 *       is never stored — only the cryptographic hash produced by the
 *       {@code IPasswordHasher} port).</li>
 *   <li>{@code role} must be non-null.</li>
 *   <li>{@code securityState} must be non-null.</li>
 * </ol>
 *
 * <p>This class is <em>immutable</em> — state transitions produce new instances
 * via the {@code with*} copy-on-write methods, preserving the audit trail and
 * enabling safe concurrent access.</p>
 *
 * <p><strong>Framework-free:</strong> No Spring, JPA, Hibernate, or Lombok annotations.</p>
 */
public final class User {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final AccountSecurityState securityState;

    /**
     * Canonical constructor — validates all business invariants.
     *
     * @param userId        the opaque UUID v4 identity
     * @param email         the user's validated email (lowercase, trimmed)
     * @param passwordHash  the BCrypt/Argon2 hash produced by the infrastructure layer
     * @param role          the authorization role
     * @param securityState the current brute-force lockout state
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if string parameters are blank
     */
    public User(UUID userId,
                String email,
                String passwordHash,
                Role role,
                AccountSecurityState securityState) {

        Objects.requireNonNull(userId, "userId must not be null");
        requireNonBlank(email, "email");
        requireNonBlank(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(securityState, "securityState must not be null");

        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.securityState = securityState;
    }

    /* ────────────────────────────────────────────────────────
       Factory — registration of new users
       ──────────────────────────────────────────────────────── */

    /**
     * Factory method for creating a newly registered user with a fresh
     * UUID v4, default CUSTOMER role, and clean security state.
     *
     * @param email        the validated email
     * @param passwordHash the hash produced by the {@code IPasswordHasher} port
     * @return a new {@link User} ready for persistence
     */
    public static User register(String email, String passwordHash) {
        return new User(
                UUID.randomUUID(),
                email,
                passwordHash,
                Role.CUSTOMER,
                AccountSecurityState.clean()
        );
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public UUID userId()                     { return userId; }
    public String email()                    { return email; }
    public String passwordHash()             { return passwordHash; }
    public Role role()                       { return role; }
    public AccountSecurityState securityState() { return securityState; }

    /* ────────────────────────────────────────────────────────
       State transitions (copy-on-write — immutable)
       ──────────────────────────────────────────────────────── */

    /**
     * Returns a new {@link User} with the given security state,
     * preserving all other fields.
     */
    public User withSecurityState(AccountSecurityState newState) {
        Objects.requireNonNull(newState, "newState must not be null");
        return new User(this.userId, this.email, this.passwordHash, this.role, newState);
    }

    /**
     * Returns a new {@link User} with an updated password hash,
     * preserving all other fields and resetting the security state
     * to clean (password change clears any lockout).
     */
    public User withPasswordHash(String newHash) {
        requireNonBlank(newHash, "newHash");
        return new User(this.userId, this.email, newHash, this.role,
                AccountSecurityState.clean());
    }

    /**
     * Returns a new {@link User} with an updated role,
     * preserving all other fields.
     */
    public User withRole(Role newRole) {
        Objects.requireNonNull(newRole, "newRole must not be null");
        return new User(this.userId, this.email, this.passwordHash, newRole,
                this.securityState);
    }

    /* ────────────────────────────────────────────────────────
       Equality by identity (UUID)
       ──────────────────────────────────────────────────────── */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    /**
     * Never includes the password hash in string representations.
     */
    @Override
    public String toString() {
        return "User[userId=%s, email=%s, role=%s, securityState=%s]"
                .formatted(userId, email, role, securityState);
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }
}
