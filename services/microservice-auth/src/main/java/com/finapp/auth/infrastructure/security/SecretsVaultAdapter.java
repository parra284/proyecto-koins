package com.finapp.auth.infrastructure.security;

import com.finapp.auth.application.port.IKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Infrastructure Adapter — connector to external secrets management
 * systems (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, etc.)
 * for loading cryptographic key material and sensitive configuration.
 *
 * <p>In this implementation, secrets are loaded from environment variables
 * (which in a Kubernetes deployment would be injected by the vault sidecar
 * or init container). This adapter can be extended to perform direct
 * HTTP calls to a vault API if needed.</p>
 *
 * <p><strong>Security contract:</strong> Private keys loaded through this
 * adapter are never logged, serialized, or exposed outside the JVM
 * memory space.</p>
 */
public class SecretsVaultAdapter {

    private static final Logger log = LoggerFactory.getLogger(SecretsVaultAdapter.class);

    /**
     * Loads an RSA key pair from PEM-encoded environment variables and
     * returns a fully initialized {@link IKeyProvider}.
     *
     * @param publicKeyPem  the PEM-encoded public key
     * @param privateKeyPem the PEM-encoded private key
     * @param keyId         the key identifier for JWKS/JWT headers
     * @return a configured {@link RsaKeypairGeneratorAdapter} loaded with
     *         the vault-provided keys
     */
    public IKeyProvider loadKeyPairFromPem(String publicKeyPem,
                                           String privateKeyPem,
                                           String keyId) {
        try {
            PublicKey publicKey = loadPublicKey(publicKeyPem);
            PrivateKey privateKey = loadPrivateKey(privateKeyPem);
            KeyPair keyPair = new KeyPair(publicKey, privateKey);

            log.info("Cryptographic key pair loaded from vault — kid={}", keyId);
            return new RsaKeypairGeneratorAdapter(keyPair, keyId);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load key pair from vault/environment", ex);
        }
    }

    /* ────────────────────────────────────────────────────────
       Private helpers
       ──────────────────────────────────────────────────────── */

    private PublicKey loadPublicKey(String pem) throws Exception {
        String cleaned = stripPemHeaders(pem,
                "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String cleaned = stripPemHeaders(pem,
                "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private static String stripPemHeaders(String pem, String header, String footer) {
        return pem
                .replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s+", "");
    }
}
