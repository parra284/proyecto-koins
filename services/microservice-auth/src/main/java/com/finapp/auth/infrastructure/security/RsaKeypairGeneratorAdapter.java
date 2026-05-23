package com.finapp.auth.infrastructure.security;

import com.finapp.auth.application.port.IKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Infrastructure Adapter — implements {@link IKeyProvider} by dynamically
 * generating or loading RSA key pairs for JWT signing.
 *
 * <p>In production, this adapter would integrate with a Hardware Security
 * Module (HSM) or vault-managed key store. For development/bootstrap, it
 * generates a fresh RSA-2048 key pair at startup.</p>
 *
 * <p><strong>Key characteristics:</strong></p>
 * <ul>
 *   <li>Algorithm: RSA</li>
 *   <li>Key size: 2048 bits (minimum for banking-grade)</li>
 *   <li>Key ID: UUID v4 generated at startup for rotation tracking</li>
 * </ul>
 */
public class RsaKeypairGeneratorAdapter implements IKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeypairGeneratorAdapter.class);
    private static final String ALGORITHM = "RS256";
    private static final int KEY_SIZE = 2048;

    private final KeyPair keyPair;
    private final String keyId;

    /**
     * Creates a new adapter, generating a fresh RSA key pair.
     * In production, this would be replaced by vault-loaded keys.
     */
    public RsaKeypairGeneratorAdapter() {
        this.keyPair = generateKeyPair();
        this.keyId = UUID.randomUUID().toString();
        log.info("RSA key pair generated — kid={}, keySize={} bits", keyId, KEY_SIZE);
    }

    /**
     * Creates an adapter with an externally provided key pair
     * (e.g., loaded from a secrets vault).
     *
     * @param keyPair the RSA key pair
     * @param keyId   the key identifier for JWKS/JWT headers
     */
    public RsaKeypairGeneratorAdapter(KeyPair keyPair, String keyId) {
        this.keyPair = keyPair;
        this.keyId = keyId;
        log.info("RSA key pair loaded from external source — kid={}", keyId);
    }

    @Override
    public byte[] publicKeyEncoded() {
        return keyPair.getPublic().getEncoded();
    }

    @Override
    public byte[] privateKeyEncoded() {
        return keyPair.getPrivate().getEncoded();
    }

    @Override
    public String activeKeyId() {
        return keyId;
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }

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
                    "RSA algorithm not available in the JVM", ex);
        }
    }
}
