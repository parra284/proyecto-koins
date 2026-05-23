package com.finapp.gateway.domain.entity;

import java.util.Objects;

/**
 * Domain Entity — maps a public-facing URL pattern to an internal target
 * service endpoint, indicating whether the route requires authentication.
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>{@code publicPathPattern} must be non-null and non-blank.</li>
 *   <li>{@code targetServiceUrl} must be non-null and non-blank.</li>
 *   <li>{@code requiresAuthentication} must be non-null.</li>
 * </ol>
 *
 * <p>This class is <em>immutable</em> — there are no setters.
 * Route changes are applied by replacing the entity instance.</p>
 *
 * <p><strong>Framework-free:</strong> No Spring, Gateway, or external annotations.</p>
 */
public final class RouteMapping {

    private final String publicPathPattern;
    private final String targetServiceUrl;
    private final boolean requiresAuthentication;

    /**
     * Canonical constructor — validates all business invariants.
     *
     * @param publicPathPattern      the URL pattern exposed to external clients (e.g., "/api/transactions/**")
     * @param targetServiceUrl       the internal service URL to route to (e.g., "http://transactions-svc:8081")
     * @param requiresAuthentication whether this route requires a valid session token
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if string parameters are blank
     */
    public RouteMapping(String publicPathPattern,
                        String targetServiceUrl,
                        Boolean requiresAuthentication) {

        requireNonBlank(publicPathPattern, "publicPathPattern");
        requireNonBlank(targetServiceUrl, "targetServiceUrl");
        Objects.requireNonNull(requiresAuthentication, "requiresAuthentication must not be null");

        this.publicPathPattern = publicPathPattern;
        this.targetServiceUrl = targetServiceUrl;
        this.requiresAuthentication = requiresAuthentication;
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public String publicPathPattern()      { return publicPathPattern; }
    public String targetServiceUrl()       { return targetServiceUrl; }
    public boolean requiresAuthentication() { return requiresAuthentication; }

    /**
     * Tests whether the given request path matches this route's public pattern.
     *
     * <p>Supports two matching strategies:</p>
     * <ul>
     *   <li>Wildcard suffix ({@code /api/v1/**}) — matches any path starting with the prefix.</li>
     *   <li>Exact match — the path must equal the pattern exactly.</li>
     * </ul>
     *
     * @param requestPath the incoming HTTP request path
     * @return {@code true} if this route handles the given path
     */
    public boolean matches(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return false;
        }
        if (publicPathPattern.endsWith("/**")) {
            String prefix = publicPathPattern.substring(0, publicPathPattern.length() - 3);
            return requestPath.startsWith(prefix);
        }
        return publicPathPattern.equals(requestPath);
    }

    /* ────────────────────────────────────────────────────────
       Equality by identity (publicPathPattern)
       ──────────────────────────────────────────────────────── */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteMapping that)) return false;
        return publicPathPattern.equals(that.publicPathPattern);
    }

    @Override
    public int hashCode() {
        return publicPathPattern.hashCode();
    }

    @Override
    public String toString() {
        return "RouteMapping[pattern=%s, target=%s, auth=%s]"
                .formatted(publicPathPattern, targetServiceUrl, requiresAuthentication);
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
