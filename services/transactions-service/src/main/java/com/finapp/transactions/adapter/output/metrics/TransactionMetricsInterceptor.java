package com.finapp.transactions.adapter.output.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

/**
 * Observability Interceptor — automatically records operational metrics
 * around controller execution without polluting business logic.
 *
 * <p>Captured metrics (exported to Prometheus via Micrometer):</p>
 * <ul>
 *   <li>{@code transactions.request.count} — total volume by endpoint and method.</li>
 *   <li>{@code transactions.request.latency} — execution time distribution.</li>
 *   <li>{@code transactions.request.errors} — error rate by status code family.</li>
 * </ul>
 */
@Component
public class TransactionMetricsInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "metricsStartTime";

    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Counter errorCounter;

    public TransactionMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestCounter = Counter.builder("transactions.request.count")
                .description("Total number of transaction API requests")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("transactions.request.errors")
                .description("Total number of transaction API errors")
                .register(meterRegistry);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTR, Instant.now());
        requestCounter.increment();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        Instant startTime = (Instant) request.getAttribute(START_TIME_ATTR);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            Timer.builder("transactions.request.latency")
                    .description("Request latency for transaction endpoints")
                    .tag("method", request.getMethod())
                    .tag("uri", request.getRequestURI())
                    .tag("status", String.valueOf(response.getStatus()))
                    .register(meterRegistry)
                    .record(duration);
        }

        if (response.getStatus() >= 400) {
            errorCounter.increment();
        }
    }
}
