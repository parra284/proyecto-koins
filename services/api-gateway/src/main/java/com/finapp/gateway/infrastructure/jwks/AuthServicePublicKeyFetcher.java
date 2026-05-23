package com.finapp.gateway.infrastructure.jwks;

import com.finapp.gateway.application.port.IPublicKeyProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Infrastructure — fetches and caches public keys from the Authentication
 * service's JWKS endpoint, implementing the {@link IPublicKeyProvider} port.
 *
 * <p><strong>Hot Cache strategy:</strong></p>
 * <ul>
 *   <li>On startup, performs a blocking initial fetch to guarantee a key is
 *       available before traffic arrives.</li>
 *   <li>A background {@link Flux#interval(Duration)} refreshes the cache
 *       at a configurable interval (default: 5 minutes).</li>
 *   <li>If a refresh fails (network error, malformed JWKS), the last known
 *       good key is preserved (fail-safe).</li>
 *   <li>All reads are lock-free via {@link AtomicReference}.</li>
 * </ul>
 *
 * <p>JWKS format parsed: RFC 7517 with RSA key type ({@code kty=RSA}).</p>
 */
public class AuthServicePublicKeyFetcher implements IPublicKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(AuthServicePublicKeyFetcher.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String jwksUrl;
    private final AtomicReference<PublicKey> primaryKey = new AtomicReference<>();
    private final AtomicReference<List<PublicKey>> allKeys = new AtomicReference<>(List.of());
    private final Disposable refreshSubscription;

    /**
     * @param webClientBuilder the shared WebClient builder
     * @param jwksUrl          the full URL of the Auth Service's JWKS endpoint
     *                         (e.g., "http://microservice-auth:8083/.well-known/jwks.json")
     * @param refreshInterval  how often to refresh the cached keys
     */
    public AuthServicePublicKeyFetcher(WebClient.Builder webClientBuilder,
                                       String jwksUrl,
                                       Duration refreshInterval) {
        this.webClient = Objects.requireNonNull(webClientBuilder, "webClientBuilder must not be null")
                .build();
        this.jwksUrl = Objects.requireNonNull(jwksUrl, "jwksUrl must not be null");

        log.info("Initializing JWKS key fetcher from: {}", jwksUrl);
        fetchAndCacheKeys()
                .doOnSuccess(v -> log.info("Initial JWKS fetch completed — {} key(s) cached", allKeys.get().size()))
                .doOnError(ex -> log.error("Initial JWKS fetch FAILED — gateway will retry on schedule", ex))
                .onErrorComplete()
                .block(Duration.ofSeconds(30));

        this.refreshSubscription = Flux.interval(refreshInterval)
                .flatMap(tick -> fetchAndCacheKeys()
                        .doOnSuccess(v -> log.debug("JWKS refresh completed — {} key(s) cached", allKeys.get().size()))
                        .doOnError(ex -> log.warn("JWKS refresh failed, retaining last known keys: {}", ex.getMessage()))
                        .onErrorComplete())
                .subscribe();

        log.info("JWKS background refresh scheduled every {}", refreshInterval);
    }

    @Override
    public Mono<PublicKey> getPublicKey() {
        PublicKey key = primaryKey.get();
        if (key == null) {
            return Mono.error(new IllegalStateException(
                    "No public key available from Auth Service JWKS — cache is empty"));
        }
        return Mono.just(key);
    }

    @Override
    public Flux<PublicKey> getAllPublicKeys() {
        List<PublicKey> keys = allKeys.get();
        if (keys.isEmpty()) {
            return Flux.error(new IllegalStateException(
                    "No public keys available from Auth Service JWKS — cache is empty"));
        }
        return Flux.fromIterable(keys);
    }

    /**
     * Shuts down the background refresh scheduler.
     * Called by Spring's destroy lifecycle.
     */
    public void destroy() {
        if (refreshSubscription != null && !refreshSubscription.isDisposed()) {
            refreshSubscription.dispose();
            log.info("JWKS background refresh stopped");
        }
    }

    /* ────────────────────────────────────────────────────────
       JWKS Fetch & Parse
       ──────────────────────────────────────────────────────── */

    private Mono<Void> fetchAndCacheKeys() {
        return webClient.get()
                .uri(jwksUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseJwksResponse)
                .doOnNext(keys -> {
                    if (!keys.isEmpty()) {
                        allKeys.set(List.copyOf(keys));
                        primaryKey.set(keys.getFirst());
                    }
                })
                .then();
    }

    /**
     * Parses a JWKS JSON response (RFC 7517) and extracts all RSA public keys.
     *
     * <p>Expected format:</p>
     * <pre>{@code
     * {
     *   "keys": [
     *     {
     *       "kty": "RSA",
     *       "use": "sig",
     *       "n": "<base64url-encoded modulus>",
     *       "e": "<base64url-encoded exponent>"
     *     }
     *   ]
     * }
     * }</pre>
     */
    private List<PublicKey> parseJwksResponse(String jwksJson) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(jwksJson);
            JsonNode keysNode = root.get("keys");

            if (keysNode == null || !keysNode.isArray()) {
                log.warn("JWKS response missing 'keys' array");
                return List.of();
            }

            List<PublicKey> parsedKeys = new ArrayList<>();
            for (JsonNode keyNode : keysNode) {
                String kty = textOrNull(keyNode, "kty");
                if (!"RSA".equals(kty)) {
                    log.debug("Skipping non-RSA key type: {}", kty);
                    continue;
                }

                String nBase64 = textOrNull(keyNode, "n");
                String eBase64 = textOrNull(keyNode, "e");

                if (nBase64 == null || eBase64 == null) {
                    log.warn("RSA key in JWKS missing 'n' or 'e' component — skipping");
                    continue;
                }

                BigInteger modulus = new BigInteger(1,
                        Base64.getUrlDecoder().decode(nBase64));
                BigInteger exponent = new BigInteger(1,
                        Base64.getUrlDecoder().decode(eBase64));

                RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
                parsedKeys.add(publicKey);

                String kid = textOrNull(keyNode, "kid");
                log.debug("Parsed RSA public key [kid={}]", kid);
            }

            return parsedKeys;

        } catch (Exception ex) {
            log.error("Failed to parse JWKS response: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && child.isTextual()) ? child.asText() : null;
    }
}
