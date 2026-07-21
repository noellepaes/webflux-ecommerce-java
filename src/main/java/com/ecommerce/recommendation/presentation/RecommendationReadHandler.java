package com.ecommerce.recommendation.presentation;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.recommendation.application.GetPurchaseRecommendationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecommendationReadHandler {

    private final GetPurchaseRecommendationsUseCase getPurchaseRecommendationsUseCase;

    public Mono<ServerResponse> getSuggestions(ServerRequest request) {
        UUID customerId;
        try {
            customerId = UUID.fromString(request.pathVariable("customerId"));
        } catch (IllegalArgumentException ex) {
            return ServerResponse.badRequest().build();
        }

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(getPurchaseRecommendationsUseCase.execute(customerId).limitRate(64), ProductDTO.class);
    }
}
