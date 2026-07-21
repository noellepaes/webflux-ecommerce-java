package com.ecommerce.product.presentation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * Ponto 1 do artigo: roteamento funcional para reads quentes de produto
 * (menos reflexão/proxy de {@code @RestController} no hot path).
 */
@Configuration
public class ProductRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> productReadRoutes(ProductReadHandler handler) {
        return RouterFunctions
                .route(GET("/api/products/{id}"), handler::getById)
                .andRoute(GET("/api/products"), handler::list);
    }
}
