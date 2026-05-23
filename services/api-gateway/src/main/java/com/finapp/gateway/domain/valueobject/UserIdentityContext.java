package com.finapp.gateway.domain.valueobject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value Object — encapsulates the authenticated identity extracted from a
 * validated session token.
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>{@code userId} must be non-null and non-blank.</li>
 *   <li>{@code roles} must be non-null, non-empty, and contain no blank entries.</li>
 * </ol>
 *
 * <p>This class is <em>completely immutable</em> — the roles list is
 * defensively copied and wrapped as unmodifiable.</p>
 *
 * <p><strong>Framework-free:</strong> No Spring, JWT, or Redis annotations.</p>
 */
public final class UserIdentityContext {

    private final String userId;
    private final List<String> roles;

    /**
     * Canonical constructor — validates all business invariants.
     *
     * @param userId the unique identifier of the authenticated user
     * @param roles  the list of authorization roles granted to the user
     * @throws NullPointerException     if {@code userId} or {@code roles} is null
     * @throws IllegalArgumentException if {@code userId} is blank, roles is empty,
     *                                  or any role entry is blank
     */
    public UserIdentityContext(String userId, List<String> roles) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(roles, "roles must not be null");

        if (userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must contain at least one role");
        }
        for (int i = 0; i < roles.size(); i++) {
            String role = roles.get(i);
            if (role == null || role.isBlank()) {
                throw new IllegalArgumentException(
                        "roles[%d] must not be null or blank".formatted(i));
            }
        }

        this.userId = userId;
        this.roles = Collections.unmodifiableList(List.copyOf(roles));
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public String userId() { return userId; }
    public List<String> roles() { return roles; }

    /**
     * Convenience check — returns {@code true} if the identity holds the
     * specified role (case-sensitive comparison).
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /* ────────────────────────────────────────────────────────
       Equality by value (userId + roles)
       ──────────────────────────────────────────────────────── */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserIdentityContext that)) return false;
        return userId.equals(that.userId) && roles.equals(that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roles);
    }

    @Override
    public String toString() {
        return "UserIdentityContext[userId=%s, roles=%s]".formatted(userId, roles);
    }
}
