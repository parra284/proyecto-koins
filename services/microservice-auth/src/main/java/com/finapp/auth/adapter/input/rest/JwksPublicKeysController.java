package com.finapp.auth.adapter.input.rest;

import com.finapp.auth.application.port.IKeyProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Zero-Trust Controller — exposes the JSON Web Key Set (JWKS) endpoint
 * so that the API Gateway and other relying parties can download the
 * Auth service's active public key for local token validation.
 *
 * <p>Endpoint: {@code GET /.well-known/jwks.json}</p>
 *
 * <p>This follows the RFC 7517 (JSON Web Key) specification. The API Gateway
 * asynchronously fetches this endpoint to validate session tokens without
 * making per-request calls to the Auth service, enabling horizontal
 * scalability under Zero-Trust principles.</p>
 */
@RestController
public class JwksPublicKeysController {

    private final IKeyProvider keyProvider;

    public JwksPublicKeysController(IKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * Returns the JWKS document containing the active RSA public key
     * in standard JWK format.
     */
    @GetMapping(value = "/.well-known/jwks.json",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        try {
            byte[] publicKeyBytes = keyProvider.publicKeyEncoded();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory
                    .getInstance("RSA")
                    .generatePublic(spec);

            String kid = keyProvider.activeKeyId();
            String algorithm = keyProvider.algorithm();

            Map<String, Object> jwk = Map.of(
                    "kty", "RSA",
                    "use", "sig",
                    "alg", algorithm,
                    "kid", kid,
                    "n", base64UrlEncode(rsaPublicKey.getModulus()),
                    "e", base64UrlEncode(rsaPublicKey.getPublicExponent())
            );

            Map<String, Object> jwks = Map.of("keys", List.of(jwk));

            return ResponseEntity.ok(jwks);

        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Encodes a {@link BigInteger} to Base64URL without padding, as
     * required by the JWK specification (RFC 7518 §6.3).
     */
    private static String base64UrlEncode(BigInteger value) {
        byte[] bytes = value.toByteArray();
        // Strip leading zero byte if present (BigInteger sign bit)
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] stripped = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, stripped, 0, stripped.length);
            bytes = stripped;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
