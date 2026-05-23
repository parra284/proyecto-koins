package com.finapp.gateway.infrastructure.config;

import com.finapp.gateway.infrastructure.security.GatewayKeypairGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Infrastructure — provisions the Gateway's RSA key pair for the
 * Zero-Trust Identity Bridge.
 *
 * <p><strong>Keys managed:</strong></p>
 * <ul>
 *   <li><strong>Gateway Private Key</strong> — used to <em>sign</em> internal
 *       application tokens (1-minute JWTs for downstream services).</li>
 *   <li><strong>Gateway Public Key</strong> — exposed via the JWKS endpoint
 *       ({@code /api/v1/.well-known/jwks.json}) so downstream microservices
 *       can verify application tokens locally at CPU speed.</li>
 * </ul>
 *
 * <p>The <em>Authentication service's</em> public key is fetched dynamically
 * via JWKS by {@link com.finapp.gateway.infrastructure.jwks.AuthServicePublicKeyFetcher}.</p>
 *
 * <p><strong>Key generation strategy:</strong> In development, the
 * {@link GatewayKeypairGenerator} auto-generates an RSA-2048 key pair at
 * startup. In production, swap to the parameterized constructor that accepts
 * vault-loaded keys.</p>
 */
@Configuration
public class PerimeterSecurityConfiguration {

    /**
     * Provisions the RSA key pair generator.
     *
     * <p>Development mode: auto-generates keys at startup.
     * Production mode: replace with {@code new GatewayKeypairGenerator(vaultKeyPair, vaultKeyId)}.</p>
     */
    @Bean
    public GatewayKeypairGenerator gatewayKeypairGenerator() {
        return new GatewayKeypairGenerator();
    }

    /**
     * Exposes the Gateway's RSA private key for injection into the
     * {@link com.finapp.gateway.adapter.output.jwt.InternalJwtSignerAdapter}.
     */
    @Bean
    public PrivateKey gatewayPrivateKey(GatewayKeypairGenerator generator) {
        return generator.privateKey();
    }

    /**
     * Exposes the Gateway's RSA public key for injection into the
     * {@link com.finapp.gateway.adapter.input.GatewayJwksController}.
     */
    @Bean
    public PublicKey gatewayPublicKey(GatewayKeypairGenerator generator) {
        return generator.publicKey();
    }
}
