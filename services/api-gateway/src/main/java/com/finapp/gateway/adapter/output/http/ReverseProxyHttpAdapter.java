package com.finapp.gateway.adapter.output.http;

import com.finapp.gateway.domain.entity.RouteMapping;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adapter — dispatches mutated HTTP requests to downstream cluster services
 * via non-blocking {@link WebClient}.
 *
 * <p>Responsibilities:</p>
 * <ol>
 *   <li>Builds the target URL using the route's path mutation rules.</li>
 *   <li>Forwards all original headers except {@code Authorization} and {@code Host}.</li>
 *   <li>Applies route-specific header transformations.</li>
 *   <li>Stamps the internal application JWT in the {@code X-Internal-Token} header.</li>
 *   <li>Streams the downstream response back to the original client (zero-copy).</li>
 * </ol>
 */
public class ReverseProxyHttpAdapter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final WebClient webClient;

    public ReverseProxyHttpAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = Objects.requireNonNull(webClientBuilder, "webClientBuilder must not be null")
                .build();
    }

    /**
     * Dispatches the incoming request to the downstream service determined
     * by the given {@link RouteMapping}, streaming the response back to the
     * original caller.
     *
     * @param exchange         the current server exchange
     * @param route            the matched route with path mutation and header transformation metadata
     * @param applicationToken the signed internal JWT to stamp (may be {@code null} for public routes)
     * @param traceId          the distributed trace ID to propagate (may be {@code null})
     * @return a {@link Mono} that completes when the response has been fully written
     */
    public Mono<Void> dispatch(ServerWebExchange exchange,
                               RouteMapping route,
                               String applicationToken,
                               String traceId) {

        ServerHttpRequest request = exchange.getRequest();
        String targetUrl = route.buildTargetUrl(request.getPath().value());
        HttpMethod method = request.getMethod();

        WebClient.RequestBodySpec requestSpec = webClient
                .method(method)
                .uri(targetUrl)
                .headers(headers -> {
                    request.getHeaders().forEach((name, values) -> {
                        if (!AUTHORIZATION_HEADER.equalsIgnoreCase(name)
                                && !HttpHeaders.HOST.equalsIgnoreCase(name)) {
                            headers.addAll(name, values);
                        }
                    });

                    route.headerTransformations().forEach(headers::set);

                    if (applicationToken != null) {
                        headers.set(INTERNAL_TOKEN_HEADER, BEARER_PREFIX + applicationToken);
                    }

                    if (traceId != null && !traceId.isBlank()) {
                        headers.set(TRACE_ID_HEADER, traceId);
                    }
                });

        Flux<DataBuffer> requestBody = request.getBody();

        return requestSpec
                .body(requestBody, DataBuffer.class)
                .exchangeToMono(clientResponse -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(clientResponse.statusCode());

                    HttpHeaders downstreamHeaders = clientResponse.headers().asHttpHeaders();
                    downstreamHeaders.forEach((name, values) -> {
                        if (!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)) {
                            response.getHeaders().addAll(name, values);
                        }
                    });

                    Flux<DataBuffer> responseBody = clientResponse.body(
                            BodyExtractors.toDataBuffers());
                    return response.writeWith(responseBody);
                });
    }
}
