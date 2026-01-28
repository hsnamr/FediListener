package com.activitypub.listener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class GatewayConfig {

    @Value("${spring.cloud.gateway.filter.request-rate-limiter.replenish-rate:10}")
    private int replenishRate;

    @Value("${spring.cloud.gateway.filter.request-rate-limiter.burst-capacity:20}")
    private int burstCapacity;

    /**
     * Configures the route locator with rate limiting for API endpoints
     * Routes /api/** requests with rate limiting applied
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter) {
        return builder.routes()
                .route(r -> r.path("/api/**")
                        .filters(f -> f.requestRateLimiter(c -> c
                                .setRateLimiter(redisRateLimiter)
                                .setKeyResolver(userKeyResolver())
                                .setDenyEmptyKey(false)
                                .setEmptyKeyStatus("429"))
                                .stripPrefix(0)) // Don't strip /api prefix
                        .uri("http://localhost:8080")) // Route to the same application
                .build();
    }

    /**
     * Redis-based rate limiter configuration
     * Replenish rate: number of requests allowed per second
     * Burst capacity: maximum number of requests allowed in a short burst
     * Values are configurable via application.properties
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }

    /**
     * Key resolver for rate limiting
     * Uses the user ID from the X-User-Id header, or falls back to IP address
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from header first
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user_" + userId);
            }
            
            // Fall back to IP address
            String ipAddress = Objects.requireNonNull(
                    exchange.getRequest().getRemoteAddress()
            ).getAddress().getHostAddress();
            return Mono.just("ip_" + ipAddress);
        };
    }
}
