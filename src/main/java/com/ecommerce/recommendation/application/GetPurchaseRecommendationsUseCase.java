package com.ecommerce.recommendation.application;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import com.ecommerce.product.domain.repository.ProductRepository;
import com.ecommerce.recommendation.config.RecommendationProperties;
import com.ecommerce.recommendation.infrastructure.ProductViewGraphRedisStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPurchaseRecommendationsUseCase {

    private final ProductRepository productRepository;
    private final ProductViewGraphRedisStore viewGraphStore;
    private final RecommendationProperties recommendationProperties;

    /**
     * Recomendação colaborativa leve: “quem viu os mesmos produtos que você também viu X”.
     * Candidatos recebem score pela frequência entre os vizinhos (co-visualizadores).
     * Se não houver sinal no Redis, cai no preenchimento com catálogo disponível fora do histórico do usuário.
     */
    public List<ProductDTO> execute(UUID customerId) {
        Set<UUID> userViews = viewGraphStore.getUserViewedProductIds(customerId);
        int limit = recommendationProperties.suggestionLimit();

        Map<UUID, Integer> scores = collaborativeScores(customerId, userViews);
        List<UUID> rankedCandidateIds = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();

        List<ProductDTO> result = new ArrayList<>();
        Set<UUID> already = new HashSet<>(userViews);

        for (UUID productId : rankedCandidateIds) {
            if (result.size() >= limit) {
                break;
            }
            if (already.contains(productId)) {
                continue;
            }
            productRepository.findById(productId)
                    .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                    .filter(Product::isAvailable)
                    .map(ProductDTO::from)
                    .ifPresent(dto -> {
                        result.add(dto);
                        already.add(productId);
                    });
        }

        if (result.size() < limit) {
            fillFromCatalogExcluding(already, limit - result.size(), result);
        }

        return result;
    }

    private Map<UUID, Integer> collaborativeScores(UUID customerId, Set<UUID> userViews) {
        Map<UUID, Integer> scores = new HashMap<>();
        if (userViews.isEmpty()) {
            return scores;
        }
        for (UUID viewedProductId : userViews) {
            Set<UUID> viewers = viewGraphStore.getProductViewerIds(viewedProductId);
            for (UUID peerCustomerId : viewers) {
                if (peerCustomerId.equals(customerId)) {
                    continue;
                }
                Set<UUID> peerViews = viewGraphStore.getUserViewedProductIds(peerCustomerId);
                for (UUID candidateId : peerViews) {
                    if (userViews.contains(candidateId)) {
                        continue;
                    }
                    scores.merge(candidateId, 1, Integer::sum);
                }
            }
        }
        return scores;
    }

    private void fillFromCatalogExcluding(Set<UUID> excludeIds, int need, List<ProductDTO> into) {
        if (need <= 0) {
            return;
        }
        productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .filter(Product::isAvailable)
                .filter(p -> !excludeIds.contains(p.getId()))
                .limit(need)
                .map(ProductDTO::from)
                .forEach(dto -> {
                    into.add(dto);
                    excludeIds.add(dto.id());
                });
    }
}
