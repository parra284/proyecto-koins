package com.finapp.gateway.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Infrastructure — loads and exposes the asymmetric RSA key pair required
 * by the Zero-Trust Identity Bridge.
 *
 * <p><strong>Keys managed:</strong></p>
 * <ul>
 *   <li><strong>Authentication Public Key</strong> — the RSA public key of the
 *       Authentication service, used to <em>verify</em> incoming session tokens.</li>
 *   <li><strong>Gateway Private Key</strong> — the RSA private key owned by the
 *       Gateway, used to <em>sign</em> internal application tokens.</li>
 * </ul>
 *
 * <p>Key material is read from environment variables (or defaults for
 * development). In production, these values should be injected from a
 * secrets vault (HashiCorp Vault, AWS Secrets Manager, etc.).</p>
 */
@Configuration
public class PerimeterSecurityConfiguration {

    @Value("${gateway.security.auth-public-key}")
    private String authPublicKeyPem;

    @Value("${gateway.security.gateway-private-key}")
    private String gatewayPrivateKeyPem;

    /**
     * Parses the Authentication service's PEM-encoded RSA public key
     * and exposes it as a Spring bean for injection into the
     * {@link com.finapp.gateway.adapter.output.jwt.ExternalJwtSessionAdapter}.
     */
    @Bean
    public PublicKey authenticationPublicKey() {
        try {
            String cleaned = stripPemHeaders(authPublicKeyPem,
                    "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException(
                    "Failed to load Authentication service public key", ex);
        }
    }

    /**
     * Parses the Gateway's PEM-encoded RSA private key and exposes it
     * as a Spring bean for injection into the
     * {@link com.finapp.gateway.adapter.output.jwt.InternalJwtSignerAdapter}.
     */
    @Bean
    public PrivateKey gatewayPrivateKey() {
        try {
            String cleaned = stripPemHeaders(gatewayPrivateKeyPem,
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException(
                    "Failed to load Gateway private key", ex);
        }
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    /**
     * Strips PEM header/footer markers and whitespace, returning the raw
     * Base64-encoded key body.
     */
    private static String stripPemHeaders(String pem, String header, String footer) {
        return pem
                .replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s+", "");
    }
}
