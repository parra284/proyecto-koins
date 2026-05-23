package com.finapp.gateway.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

/**
 * Infrastructure — generates or holds the Gateway's RSA key pair used
 * for signing internal application JWTs (Zero-Trust Identity Bridge).
 *
 * <p><strong>Two operational modes:</strong></p>
 * <ul>
 *   <li><strong>Development/bootstrap:</strong> The no-arg constructor
 *       generates a fresh RSA-2048 key pair in memory at startup.
 *       Keys are ephemeral — each restart produces a new pair.
 *       Downstream services refresh automatically via the JWKS endpoint.</li>
 *   <li><strong>Production:</strong> The parameterized constructor accepts
 *       an externally provisioned key pair (e.g., loaded from HashiCorp Vault,
 *       AWS Secrets Manager, or a Hardware Security Module).</li>
 * </ul>
 *
 * <p><strong>Key characteristics:</strong></p>
 * <ul>
 *   <li>Algorithm: RSA</li>
 *   <li>Key size: 2048 bits (minimum for banking-grade security)</li>
 *   <li>Key ID: UUID v4 generated at startup for JWKS rotation tracking</li>
 * </ul>
 */
public class GatewayKeypairGenerator {

    private static final Logger log = LoggerFactory.getLogger(GatewayKeypairGenerator.class);
    private static final int KEY_SIZE = 2048;

    private final KeyPair keyPair;
    private final String keyId;

    /**
     * Development constructor — generates a fresh RSA-2048 key pair in memory.
     * Each application restart produces a new key pair; downstream services
     * detect the change automatically via JWKS refresh.
     */
    public GatewayKeypairGenerator() {
        this.keyPair = generateKeyPair();
        this.keyId = UUID.randomUUID().toString();
        log.info("Gateway RSA key pair generated — kid={}, keySize={} bits", keyId, KEY_SIZE);
    }

    /**
     * Production constructor — accepts a pre-loaded key pair from an
     * external secrets provider (vault, HSM, environment).
     *
     * @param keyPair the RSA key pair loaded from a secure source
     * @param keyId   the key identifier for JWKS and JWT {@code kid} headers
     */
    public GatewayKeypairGenerator(KeyPair keyPair, String keyId) {
        this.keyPair = keyPair;
        this.keyId = keyId;
        log.info("Gateway RSA key pair loaded from external source — kid={}", keyId);
    }

    /* ────────────────────────────────────────────────────────
       Accessors
       ──────────────────────────────────────────────────────── */

    public PrivateKey privateKey() { return keyPair.getPrivate(); }
    public PublicKey publicKey()   { return keyPair.getPublic(); }
    public String activeKeyId()   { return keyId; }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "RSA algorithm not available in the JVM — cannot generate Gateway key pair", ex);
        }
    }
}
