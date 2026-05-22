package com.finapp.transactions.adapter.input.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * W3C Trace Context Filter — distributed tracing middleware.
 *
 * <p>Reads the {@code traceparent} header (W3C Trace Context standard) from
 * incoming requests. If present, extracts the {@code traceId} and propagates
 * it through the SLF4J MDC so that every log entry within this request
 * scope carries the same correlation identifier.</p>
 *
 * <p>If no trace header is found, a new {@code traceId} is generated to
 * ensure full traceability even for requests that originate outside the mesh.</p>
 *
 * <p>Runs after the JWT filter ({@link Order} = 2).</p>
 */
@Component
@Order(2)
public class TraceContextFilter extends OncePerRequestFilter {

    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = extractTraceId(request);
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        // Propagate the traceId in the response header for upstream callers
        response.setHeader("X-Trace-Id", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * Parses the W3C {@code traceparent} header format:
     * {@code version-traceId-parentId-traceFlags}
     * (e.g. {@code 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01}).
     *
     * <p>Falls back to a random UUID if the header is missing or malformed.</p>
     */
    private String extractTraceId(HttpServletRequest request) {
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        if (traceparent != null && !traceparent.isBlank()) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2) {
                return parts[1]; // traceId field
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
