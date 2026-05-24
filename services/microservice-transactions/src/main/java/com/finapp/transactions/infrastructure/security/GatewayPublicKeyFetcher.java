package com.finapp.transactions.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finapp.transactions.adapter.input.filter.IGatewayKeyProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway Public Key Fetcher — JWKS infrastructure adapter.
 *
 * <p>
 * Implements the {@link IGatewayKeyProvider} contract by consuming the
 * API Gateway's JWKS (JSON Web Key Set) endpoint at bootstrap time.
 * The downloaded RSA public key is deserialized from its JWK representation
 * ({@code n} modulus + {@code e} exponent) into a native {@link PublicKey}
 * and cached in an {@link AtomicReference} for thread-safe, lock-free
 * access during request processing.
 * </p>
 *
 * <h3>Bootstrap Behavior (Fail-Fast)</h3>
 * <p>If the JWKS endpoint is unreachable or returns malformed data during
 * application startup, an {@link IllegalStateException} is thrown, which
 * prevents the Spring Boot container from completing initialization.
 * This is intentional: under Zero Trust, a microservice that cannot
 * verify tokens must <strong>not</strong> accept traffic.</p>
 *
 * <h3>Clean Architecture Placement</h3>
 * <p>This class belongs to <strong>Layer 4 (Infrastructure)</strong>.
 * It is the only component in the system that knows about HTTP endpoints,
 * JSON parsing, and JWK deserialization. The security filter in Layer 3
 * interacts exclusively through the {@link IGatewayKeyProvider} port.</p>
 *
 * @see IGatewayKeyProvider
 */
@Component
public class GatewayPublicKeyFetcher implements IGatewayKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(GatewayPublicKeyFetcher.class);

    private final String jwksUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicReference<PublicKey> cachedKey = new AtomicReference<>();

    public GatewayPublicKeyFetcher(
            @Value("${app.gateway.jwks-url}") String jwksUrl) {
        this.jwksUrl = jwksUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Bootstrap hook — executes a single synchronous HTTP call to the
     * API Gateway's JWKS endpoint immediately after bean construction.
     *
     * <p>Downloads the JWK Set, extracts the first RSA key, converts it
     * to a native {@link PublicKey}, and stores it in the in-memory cache.</p>
     *
     * @throws IllegalStateException if the endpoint is unreachable,
     *                               the response is malformed, or no
     *                               RSA key is found in the JWK Set
     */
    @PostConstruct
    void fetchAndCachePublicKey() {
        log.info("Fetching JWKS from API Gateway: {}", jwksUrl);

        try {
            // ── HTTP call to JWKS endpoint ──────────────────────────
            String jwksJson = restTemplate.getForObject(jwksUrl, String.class);

            if (jwksJson == null || jwksJson.isBlank()) {
                throw new IllegalStateException("JWKS endpoint returned empty response");
            }

            // ── Deserialize JWK Set ─────────────────────────────────
            JsonNode jwks = objectMapper.readTree(jwksJson);
            JsonNode keys = jwks.get("keys");

            if (keys == null || !keys.isArray() || keys.isEmpty()) {
                throw new IllegalStateException("JWKS does not contain a 'keys' array or it is empty");
            }

            // ── Find the first RSA key ──────────────────────────────
            JsonNode rsaKey = findFirstRsaKey(keys);
            PublicKey publicKey = buildRsaPublicKey(rsaKey);

            cachedKey.set(publicKey);
            log.info("JWKS public key loaded and cached successfully (kid: {})",
                    rsaKey.has("kid") ? rsaKey.get("kid").asText() : "N/A");

        } catch (IllegalStateException e) {
            throw e; // re-throw domain exceptions as-is
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to fetch or parse JWKS from API Gateway at [%s]: %s"
                            .formatted(jwksUrl, e.getMessage()), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the cached public key with near-zero latency.
     * No network call is performed; the key was pre-loaded at bootstrap.</p>
     */
    @Override
    public PublicKey getActivePublicKey() {
        PublicKey key = cachedKey.get();
        if (key == null) {
            throw new IllegalStateException(
                    "No public key available — JWKS was not loaded during bootstrap");
        }
        return key;
    }

    /*
     * ────────────────────────────────────────────────────────
     * Private helpers — JWK deserialization
     * ────────────────────────────────────────────────────────
     */

    /**
     * Iterates the {@code keys} array and returns the first JWK
     * with {@code "kty": "RSA"}.
     */
    private JsonNode findFirstRsaKey(JsonNode keys) {
        for (JsonNode key : keys) {
            if (key.has("kty") && "RSA".equalsIgnoreCase(key.get("kty").asText())) {
                return key;
            }
        }
        throw new IllegalStateException("No RSA key found in JWKS key set");
    }

    /**
     * Converts a JWK JSON node into a native {@link PublicKey}.
     *
     * <p>Extracts the Base64url-encoded {@code n} (modulus) and {@code e}
     * (exponent) fields and reconstructs the RSA public key using the
     * standard {@link KeyFactory} API.</p>
     *
     * @param jwk the JSON node representing a single JWK with kty=RSA
     * @return the reconstructed RSA public key
     */
    private PublicKey buildRsaPublicKey(JsonNode jwk) {
        try {
            String modulusB64 = jwk.get("n").asText();
            String exponentB64 = jwk.get("e").asText();

            byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusB64);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentB64);

            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build RSA PublicKey from JWK: " + e.getMessage(), e);
        }
    }
}
