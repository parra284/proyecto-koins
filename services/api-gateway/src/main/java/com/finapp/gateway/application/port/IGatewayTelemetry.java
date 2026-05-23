package com.finapp.gateway.application.port;

/**
 * Output Port — abstracts the gateway's observability concerns:
 * traffic metrics recording and distributed trace context propagation.
 *
 * <p>Implementations connect to concrete observability stacks
 * (Micrometer, OpenTelemetry, Prometheus) while keeping the
 * application core infrastructure-agnostic.</p>
 *
 * <p><strong>Framework-free interface.</strong></p>
 */
public interface IGatewayTelemetry {

    /**
     * Records a successfully routed request with its latency.
     *
     * @param routePattern the matched route pattern (e.g., "/api/transactions/**")
     * @param method       the HTTP method (GET, POST, etc.)
     * @param durationMs   the total gateway processing time in milliseconds
     */
    void recordRequestRouted(String routePattern, String method, long durationMs);

    /**
     * Records a blocked request (rate-limited, unauthorized, etc.).
     *
     * @param reason a short classifier for the block cause
     *               (e.g., "RateLimitExceeded", "InvalidSessionToken")
     */
    void recordRequestBlocked(String reason);

    /**
     * Generates a new trace ID or propagates an existing one from
     * an incoming request header.
     *
     * <p>If {@code incomingTraceId} is non-null and non-blank, it is
     * returned as-is for propagation continuity. Otherwise, a new
     * unique trace ID is generated.</p>
     *
     * @param incomingTraceId the trace ID from the incoming request
     *                        headers (may be {@code null})
     * @return the trace ID to propagate to downstream services
     */
    String propagateTraceId(String incomingTraceId);
}
