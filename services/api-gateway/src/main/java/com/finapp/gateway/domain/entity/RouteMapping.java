package com.finapp.gateway.domain.entity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain Entity — maps a public-facing URL pattern to an internal target
 * service endpoint, carrying metadata for header transformation and path
 * mutation (prefix stripping).
 *
 * <p><strong>Invariants enforced at construction time:</strong></p>
 * <ol>
 *   <li>{@code publicPathPattern} must be non-null and non-blank.</li>
 *   <li>{@code targetServiceUrl} must be non-null and non-blank.</li>
 *   <li>{@code requiresAuthentication} must be non-null.</li>
 *   <li>{@code headerTransformations} is defensively copied and unmodifiable.</li>
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
    private final String stripPrefix;
    private final Map<String, String> headerTransformations;

    /**
     * Canonical constructor — validates all business invariants.
     *
     * @param publicPathPattern      the URL pattern exposed to external clients (e.g., "/api/transactions/**")
     * @param targetServiceUrl       the internal service URL to route to (e.g., "http://transactions-svc:8081")
     * @param requiresAuthentication whether this route requires a valid session token
     * @param stripPrefix            optional prefix to remove from the request path before forwarding
     *                               (e.g., "/api/transactions"); may be {@code null} for no stripping
     * @param headerTransformations  additional headers to inject into the downstream request;
     *                               may be {@code null} or empty
     * @throws NullPointerException     if required parameters are null
     * @throws IllegalArgumentException if string parameters are blank
     */
    public RouteMapping(String publicPathPattern,
                        String targetServiceUrl,
                        Boolean requiresAuthentication,
                        String stripPrefix,
                        Map<String, String> headerTransformations) {

        requireNonBlank(publicPathPattern, "publicPathPattern");
        requireNonBlank(targetServiceUrl, "targetServiceUrl");
        Objects.requireNonNull(requiresAuthentication, "requiresAuthentication must not be null");

        this.publicPathPattern = publicPathPattern;
        this.targetServiceUrl = targetServiceUrl;
        this.requiresAuthentication = requiresAuthentication;
        this.stripPrefix = stripPrefix;
        this.headerTransformations = headerTransformations == null || headerTransformations.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headerTransformations));
    }

    /* ────────────────────────────────────────────────────────
       Accessors (read-only)
       ──────────────────────────────────────────────────────── */

    public String publicPathPattern()                { return publicPathPattern; }
    public String targetServiceUrl()                 { return targetServiceUrl; }
    public boolean requiresAuthentication()          { return requiresAuthentication; }
    public String stripPrefix()                      { return stripPrefix; }
    public Map<String, String> headerTransformations() { return headerTransformations; }

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

    /**
     * Applies prefix stripping to the given request path.
     *
     * <p>If {@code stripPrefix} is configured and the request path starts with it,
     * the prefix is removed. Otherwise, the original path is returned unchanged.</p>
     *
     * <p>Examples (with {@code stripPrefix = "/api/transactions"}):</p>
     * <ul>
     *   <li>{@code "/api/transactions/123"} → {@code "/123"}</li>
     *   <li>{@code "/api/transactions"}     → {@code "/"}</li>
     *   <li>{@code "/other/path"}           → {@code "/other/path"} (no match)</li>
     * </ul>
     *
     * @param requestPath the original incoming HTTP request path
     * @return the mutated path with the prefix stripped, or the original if no stripping applies
     */
    public String mutatedPath(String requestPath) {
        if (stripPrefix == null || stripPrefix.isBlank()) {
            return requestPath;
        }
        if (requestPath.startsWith(stripPrefix)) {
            String mutated = requestPath.substring(stripPrefix.length());
            return mutated.isEmpty() ? "/" : mutated;
        }
        return requestPath;
    }

    /**
     * Constructs the fully-qualified downstream URL by combining the
     * target service base URL with the mutated (prefix-stripped) request path.
     *
     * @param requestPath the original incoming HTTP request path
     * @return the full URL to dispatch to the downstream service
     */
    public String buildTargetUrl(String requestPath) {
        String baseUrl = targetServiceUrl;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + mutatedPath(requestPath);
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
        return "RouteMapping[pattern=%s, target=%s, auth=%s, strip=%s, headers=%d]"
                .formatted(publicPathPattern, targetServiceUrl, requiresAuthentication,
                        stripPrefix, headerTransformations.size());
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
