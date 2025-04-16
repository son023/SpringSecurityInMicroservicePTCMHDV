package com.example.ftgoapigateway.config;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;


@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("fallback-route", r -> r.path("/fallback/**")
                        .filters(f -> f.setStatus(HttpStatus.SERVICE_UNAVAILABLE))
                        .uri("no://op"))
                .build();
    }
}

