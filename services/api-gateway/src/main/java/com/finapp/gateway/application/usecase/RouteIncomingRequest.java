package com.finapp.gateway.application.usecase;

import com.finapp.gateway.application.port.IRouteMappingRepository;
import com.finapp.gateway.domain.entity.RouteMapping;
import com.finapp.gateway.domain.exception.RouteNotFoundException;

import java.util.Objects;

/**
 * Query Interactor — resolves the downstream target service for an
 * incoming request path using the configured route catalogue.
 *
 * <p>This interactor encapsulates the routing decision: given a request
 * path, it queries the {@link IRouteMappingRepository} for a matching
 * {@link RouteMapping} and returns the full routing context needed by
 * the front controller to dispatch the request.</p>
 *
 * <p><strong>No Spring annotations.</strong> Wired via constructor injection
 * in the infrastructure configuration.</p>
 */
public class RouteIncomingRequest {

    private final IRouteMappingRepository routeMappingRepository;

    public RouteIncomingRequest(IRouteMappingRepository routeMappingRepository) {
        this.routeMappingRepository = Objects.requireNonNull(
                routeMappingRepository, "routeMappingRepository must not be null");
    }

    /**
     * Result carrying the resolved route and the fully-qualified target URL.
     *
     * @param route     the matched {@link RouteMapping} entity
     * @param targetUrl the concrete URL to dispatch to (serviceUrl + request path suffix)
     */
    public record Result(RouteMapping route, String targetUrl) {
        public Result {
            Objects.requireNonNull(route, "route must not be null");
            Objects.requireNonNull(targetUrl, "targetUrl must not be null");
            if (targetUrl.isBlank()) {
                throw new IllegalArgumentException("targetUrl must not be blank");
            }
        }
    }

    /**
     * Resolves the route for the given request path.
     *
     * <p>The target URL is constructed by appending the request path to the
     * service base URL defined in the matching {@link RouteMapping}.</p>
     *
     * @param requestPath the incoming HTTP request path (e.g., "/api/transactions/123")
     * @return a {@link Result} containing the matched route and the resolved target URL
     * @throws RouteNotFoundException   if no route matches the given path
     * @throws NullPointerException     if {@code requestPath} is null
     * @throws IllegalArgumentException if {@code requestPath} is blank
     */
    public Result execute(String requestPath) {
        Objects.requireNonNull(requestPath, "requestPath must not be null");
        if (requestPath.isBlank()) {
            throw new IllegalArgumentException("requestPath must not be blank");
        }

        RouteMapping route = routeMappingRepository.findByPath(requestPath)
                .orElseThrow(() -> new RouteNotFoundException(requestPath));

        String targetUrl = buildTargetUrl(route, requestPath);

        return new Result(route, targetUrl);
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Constructs the downstream URL by appending the request path to the
     * target service's base URL, stripping any trailing slash from the base.
     */
    private String buildTargetUrl(RouteMapping route, String requestPath) {
        String baseUrl = route.targetServiceUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + requestPath;
    }
}
