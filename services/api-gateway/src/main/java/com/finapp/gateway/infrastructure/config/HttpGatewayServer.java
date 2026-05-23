package com.finapp.gateway.infrastructure.config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Infrastructure — configures the non-blocking HTTP server (Netty)
 * for the API Gateway.
 *
 * <p>Spring Boot WebFlux automatically selects Netty as the default
 * reactive server. This configuration makes the choice explicit and
 * provides extension points for production tuning (worker threads,
 * connection limits, timeouts).</p>
 */
@Configuration
public class HttpGatewayServer {

    /**
     * Explicitly declares the reactive web server factory backed by Netty,
     * enabling fine-grained tuning of the event-loop configuration.
     */
    @Bean
    public ReactiveWebServerFactory reactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        // Production tuning: adjust worker threads, connection limits, etc.
        // factory.addServerCustomizers(httpServer ->
        //     httpServer.runOn(LoopResources.create("gateway-loop", 4, true))
        // );
        return factory;
    }

    /**
     * WebClient builder configured for downstream service communication.
     * Centralized here to enforce consistent timeouts and header propagation.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)); // 2 MB buffer
    }
}
