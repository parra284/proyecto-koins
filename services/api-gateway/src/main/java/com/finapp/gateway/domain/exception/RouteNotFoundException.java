package com.finapp.gateway.domain.exception;

/**
 * Thrown when no {@link com.finapp.gateway.domain.entity.RouteMapping}
 * matches the incoming request path.
 */
public final class RouteNotFoundException extends GatewayDomainException {

    private final String requestPath;

    public RouteNotFoundException(String requestPath) {
        super("No route found for path: " + requestPath);
        this.requestPath = requestPath;
    }

    public String requestPath() { return requestPath; }
}
