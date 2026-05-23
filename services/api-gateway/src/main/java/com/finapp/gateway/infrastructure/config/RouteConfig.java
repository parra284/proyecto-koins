package com.finapp.gateway.infrastructure.config;

import com.finapp.gateway.application.port.IRouteMappingRepository;
import com.finapp.gateway.domain.entity.RouteMapping;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Infrastructure — loads route mappings from the application configuration
 * (YAML) and exposes an {@link IRouteMappingRepository} implementation
 * backed by the in-memory route catalogue.
 *
 * <p>Routes are defined under {@code gateway.routes} in
 * {@code application.yml}.</p>
 */
@Configuration
public class RouteConfig {

    /**
     * Binds the {@code gateway.routes} YAML list to a strongly-typed
     * properties object.
     */
    @ConfigurationProperties(prefix = "gateway")
    public record GatewayRoutesProperties(List<RouteEntry> routes) {
        public record RouteEntry(
                String publicPathPattern,
                String targetServiceUrl,
                boolean requiresAuthentication
        ) {}
    }

    @Bean
    @ConfigurationProperties(prefix = "gateway")
    public GatewayRoutesProperties gatewayRoutesProperties() {
        return new GatewayRoutesProperties(new ArrayList<>());
    }

    /**
     * Converts the YAML-bound route entries into domain {@link RouteMapping}
     * entities and provides an in-memory repository implementation.
     */
    @Bean
    public IRouteMappingRepository routeMappingRepository(GatewayRoutesProperties properties) {
        List<RouteMapping> routes = properties.routes().stream()
                .map(entry -> new RouteMapping(
                        entry.publicPathPattern(),
                        entry.targetServiceUrl(),
                        entry.requiresAuthentication()))
                .toList();

        List<RouteMapping> immutableRoutes = Collections.unmodifiableList(routes);

        return new IRouteMappingRepository() {
            @Override
            public Optional<RouteMapping> findByPath(String requestPath) {
                return immutableRoutes.stream()
                        .filter(route -> route.matches(requestPath))
                        .findFirst();
            }

            @Override
            public List<RouteMapping> findAll() {
                return immutableRoutes;
            }
        };
    }
}
