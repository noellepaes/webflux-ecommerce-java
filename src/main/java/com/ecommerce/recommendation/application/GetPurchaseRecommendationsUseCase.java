package com.ecommerce.recommendation.application;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import com.ecommerce.product.domain.repository.ProductRepository;
import com.ecommerce.recommendation.config.RecommendationProperties;
import com.ecommerce.recommendation.infrastructure.ProductViewGraphRedisStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GetPurchaseRecommendationsUseCase {

    private final ProductRepository productRepository;
    private final ProductViewGraphRedisStore viewGraphStore;
    private final RecommendationProperties recommendationProperties;

    public Flux<ProductDTO> execute(UUID customerId) {
        int limit = recommendationProperties.suggestionLimit();

        return viewGraphStore.getUserViewedProductIds(customerId)
                .collect(HashSet::new, Set::add)
                .flatMapMany(userViews -> collaborativeScores(customerId, userViews)
                        .flatMapMany(scores -> {
                            Flux<UUID> ranked = Flux.fromIterable(scores.entrySet())
                                    .sort(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                                    .map(Map.Entry::getKey);

                            Set<UUID> already = new HashSet<>(userViews);

                            return ranked
                                    .filter(id -> !already.contains(id))
                                    .concatMap(productId -> productRepository.findById(productId)
                                            .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                                            .filter(Product::isAvailable)
                                            .map(ProductDTO::from)
                                            .doOnNext(dto -> already.add(dto.id())))
                                    .take(limit)
                                    .collectList()
                                    .flatMapMany(result -> {
                                        if (result.size() >= limit) {
                                            return Flux.fromIterable(result);
                                        }
                                        return Flux.fromIterable(result)
                                                .concatWith(fillFromCatalogExcluding(already, limit - result.size()));
                                    });
                        }));
    }

    private Mono<Map<UUID, Integer>> collaborativeScores(UUID customerId, Set<UUID> userViews) {
        if (userViews.isEmpty()) {
            return Mono.just(Map.of());
        }

        Map<UUID, Integer> scores = new ConcurrentHashMap<>();

        return Flux.fromIterable(userViews)
                .flatMap(viewedProductId -> viewGraphStore.getProductViewerIds(viewedProductId)
                        .filter(peerId -> !peerId.equals(customerId))
                        .flatMap(peerCustomerId -> viewGraphStore.getUserViewedProductIds(peerCustomerId)
                                .filter(candidateId -> !userViews.contains(candidateId))
                                .doOnNext(candidateId -> scores.merge(candidateId, 1, Integer::sum))))
                .then(Mono.just(scores));
    }

    private Flux<ProductDTO> fillFromCatalogExcluding(Set<UUID> excludeIds, int need) {
        if (need <= 0) {
            return Flux.empty();
        }
        return productRepository.findByStatus(ProductStatus.ACTIVE)
                .filter(Product::isAvailable)
                .filter(p -> !excludeIds.contains(p.getId()))
                .take(need)
                .map(ProductDTO::from);
    }
}
