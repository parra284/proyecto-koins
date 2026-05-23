package com.finapp.gateway.adapter.input;

import com.finapp.gateway.infrastructure.security.GatewayKeypairGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST Controller — exposes the Gateway's public signing key in the
 * standard JWKS (JSON Web Key Set) format (RFC 7517).
 *
 * <p><strong>Intended audience:</strong> Internal cluster services only
 * ({@code Transactions}, {@code Analysis}). These microservices download
 * this key set to locally verify the short-lived application JWTs issued
 * by the Gateway, eliminating per-request network calls to a centralized
 * auth endpoint.</p>
 *
 * <p>This endpoint should be restricted to the private cluster network
 * via infrastructure-level firewall rules or Kubernetes NetworkPolicy —
 * it must NOT be exposed to external clients.</p>
 */
@RestController
@RequestMapping("/api/v1/.well-known")
public class GatewayJwksController {

    private final PublicKey gatewayPublicKey;
    private final String keyId;

    /**
     * @param gatewayPublicKey   the Gateway's RSA public key (paired with the
     *                           private key used by {@code InternalJwtSignerAdapter})
     * @param keypairGenerator   provides the dynamic key ID for JWKS rotation tracking
     */
    public GatewayJwksController(PublicKey gatewayPublicKey,
                                  GatewayKeypairGenerator keypairGenerator) {
        this.gatewayPublicKey = Objects.requireNonNull(
                gatewayPublicKey, "gatewayPublicKey must not be null");
        this.keyId = Objects.requireNonNull(
                keypairGenerator, "keypairGenerator must not be null").activeKeyId();
    }

    /**
     * Returns the Gateway's public key set in JWKS format.
     *
     * <p>Response format:</p>
     * <pre>{@code
     * {
     *   "keys": [
     *     {
     *       "kty": "RSA",
     *       "use": "sig",
     *       "alg": "RS256",
     *       "kid": "api-gateway-signing-key",
     *       "n": "<base64url modulus>",
     *       "e": "<base64url exponent>"
     *     }
     *   ]
     * }
     * }</pre>
     */
    @GetMapping(value = "/jwks.json", produces = "application/json")
    public Map<String, Object> jwks() {
        RSAPublicKey rsaKey = (RSAPublicKey) gatewayPublicKey;

        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", keyId);
        jwk.put("n", toBase64Url(rsaKey.getModulus().toByteArray()));
        jwk.put("e", toBase64Url(rsaKey.getPublicExponent().toByteArray()));

        return Map.of("keys", List.of(jwk));
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Converts a {@link java.math.BigInteger#toByteArray()} result to
     * Base64url encoding, stripping the leading zero byte that
     * {@code BigInteger} adds for unsigned representation of positive numbers.
     */
    private static String toBase64Url(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] stripped = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, stripped, 0, stripped.length);
            bytes = stripped;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
