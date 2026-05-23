package com.ecommerce.recommendation.presentation;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.recommendation.application.GetPurchaseRecommendationsUseCase;
import com.ecommerce.recommendation.infrastructure.ProductViewGraphRedisStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recomendações", description = "Visualizações em Redis (SETs bidirecionais) e sugestões colaborativas; TTL configurável")
public class RecommendationController {

    private final GetPurchaseRecommendationsUseCase getPurchaseRecommendationsUseCase;
    private final ProductViewGraphRedisStore viewGraphStore;

    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Sugestões para o cliente",
            description = "Prioriza produtos que outros clientes viram junto com o seu histórico; completa com catálogo se faltar sinal")
    @ApiResponse(responseCode = "200", description = "Lista de sugestões")
    public ResponseEntity<List<ProductDTO>> getSuggestions(
            @Parameter(description = "ID do cliente", required = true) @PathVariable UUID customerId) {
        return ResponseEntity.ok(getPurchaseRecommendationsUseCase.execute(customerId));
    }

    @PostMapping("/customers/{customerId}/views")
    @Operation(summary = "Registrar visualização ou clique",
            description = "Atualiza user:{id}→produtos e product:{id}→usuários no Redis; renova TTL")
    @ApiResponse(responseCode = "204", description = "Registrado")
    public ResponseEntity<Void> recordView(
            @Parameter(description = "ID do cliente", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody RecordProductViewRequest request) {
        viewGraphStore.recordView(customerId, request.productId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
