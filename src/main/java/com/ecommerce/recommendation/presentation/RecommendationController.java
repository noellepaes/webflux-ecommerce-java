package com.ecommerce.recommendation.presentation;

import com.ecommerce.recommendation.infrastructure.ProductViewGraphRedisStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Writes de views. O GET de sugestões fica no roteamento funcional
 * ({@link RecommendationRouterConfig} + {@link RecommendationReadHandler}).
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recomendações", description = "POST views; GET sugestões via RouterFunction")
public class RecommendationController {

    private final ProductViewGraphRedisStore viewGraphStore;

    @PostMapping("/customers/{customerId}/views")
    @Operation(summary = "Registrar visualização ou clique",
            description = "Atualiza user:{id}→produtos e product:{id}→usuários no Redis; renova TTL")
    @ApiResponse(responseCode = "204", description = "Registrado")
    public Mono<ResponseEntity<Void>> recordView(
            @Parameter(description = "ID do cliente", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody RecordProductViewRequest request) {
        return viewGraphStore.recordView(customerId, request.productId())
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }
}
