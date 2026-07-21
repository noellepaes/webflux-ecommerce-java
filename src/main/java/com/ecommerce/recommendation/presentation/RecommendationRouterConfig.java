package com.ecommerce.recommendation.presentation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class RecommendationRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> recommendationReadRoutes(RecommendationReadHandler handler) {
        return RouterFunctions.route(
                GET("/api/recommendations/customers/{customerId}"),
                handler::getSuggestions);
    }
}
