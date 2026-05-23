package com.finapp.gateway.adapter.output.telemetry;

import com.finapp.gateway.application.port.IGatewayTelemetry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Adapter — implements {@link IGatewayTelemetry} by connecting to
 * Micrometer for metrics and OpenTelemetry-compatible trace ID propagation.
 *
 * <p><strong>Metrics registered:</strong></p>
 * <ul>
 *   <li>{@code gateway.requests.routed} — counter of successfully proxied requests,
 *       tagged by {@code route} and {@code method}.</li>
 *   <li>{@code gateway.requests.blocked} — counter of rejected requests,
 *       tagged by {@code reason}.</li>
 *   <li>{@code gateway.requests.duration} — timer recording end-to-end latency
 *       of routed requests, tagged by {@code route} and {@code method}.</li>
 * </ul>
 *
 * <p><strong>Trace propagation:</strong> If an incoming {@code X-Trace-Id}
 * header is present, it is propagated as-is. Otherwise, a new UUID-based
 * trace ID is generated for the request lifecycle.</p>
 */
public class GatewayMetricsAndTracingAdapter implements IGatewayTelemetry {

    private static final String METRIC_REQUESTS_ROUTED = "gateway.requests.routed";
    private static final String METRIC_REQUESTS_BLOCKED = "gateway.requests.blocked";
    private static final String METRIC_REQUESTS_DURATION = "gateway.requests.duration";

    private final MeterRegistry meterRegistry;

    public GatewayMetricsAndTracingAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry, "meterRegistry must not be null");
    }

    @Override
    public void recordRequestRouted(String routePattern, String method, long durationMs) {
        Counter.builder(METRIC_REQUESTS_ROUTED)
                .tag("route", sanitizeTag(routePattern))
                .tag("method", method)
                .register(meterRegistry)
                .increment();

        Timer.builder(METRIC_REQUESTS_DURATION)
                .tag("route", sanitizeTag(routePattern))
                .tag("method", method)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordRequestBlocked(String reason) {
        Counter.builder(METRIC_REQUESTS_BLOCKED)
                .tag("reason", sanitizeTag(reason))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public String propagateTraceId(String incomingTraceId) {
        if (incomingTraceId != null && !incomingTraceId.isBlank()) {
            return incomingTraceId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Sanitizes a tag value to prevent high-cardinality metric explosion.
     */
    private static String sanitizeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() > 128 ? value.substring(0, 128) : value;
    }
}
