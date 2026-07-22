package com.ecommerce.recommendation.application;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import com.ecommerce.product.infrastructure.repository.ProductRepository;
import com.ecommerce.recommendation.config.RecommendationProperties;
import com.ecommerce.recommendation.infrastructure.ProductViewGraphRedisStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recomendações colaborativas.
 * Hidratação via {@code findById} no service (repositório só {@code ReactiveCrudRepository}).
 */
@Service
@RequiredArgsConstructor
public class GetPurchaseRecommendationsUseCase {

    private static final int REDIS_FANOUT_CONCURRENCY = 16;

    private final ProductRepository productRepository;
    private final ProductViewGraphRedisStore viewGraphStore;
    private final RecommendationProperties recommendationProperties;

    public Flux<ProductDTO> execute(UUID customerId) {
        int limit = recommendationProperties.suggestionLimit();

        return viewGraphStore.getUserViewedProductIds(customerId)
                .collectList()
                .map(list -> (Set<UUID>) new HashSet<>(list))
                .flatMapMany(userViews -> collaborativeScores(customerId, userViews)
                        .flatMapMany(scores -> {
                            List<UUID> rankedIds = scores.entrySet().stream()
                                    .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                                    .map(Map.Entry::getKey)
                                    .filter(id -> !userViews.contains(id))
                                    .toList();

                            Set<UUID> exclude = new HashSet<>(userViews);
                            return hydrateRanked(rankedIds, limit, exclude)
                                    .collectList()
                                    .flatMapMany(result -> {
                                        if (result.size() >= limit) {
                                            return Flux.fromIterable(result).limitRate(64);
                                        }
                                        exclude.addAll(result.stream().map(ProductDTO::id).toList());
                                        return Flux.fromIterable(result)
                                                .concatWith(fillFromCatalogExcluding(exclude, limit - result.size()))
                                                .limitRate(64);
                                    });
                        }));
    }

    private Flux<ProductDTO> hydrateRanked(List<UUID> rankedIds, int limit, Set<UUID> exclude) {
        if (rankedIds.isEmpty() || limit <= 0) {
            return Flux.empty();
        }
        List<UUID> candidates = rankedIds.stream()
                .filter(id -> !exclude.contains(id))
                .limit(Math.max(limit * 3L, limit))
                .toList();
        if (candidates.isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(candidates)
                .concatMap(id -> productRepository.findById(id)
                        .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                        .filter(Product::isAvailable)
                        .map(ProductDTO::from))
                .take(limit);
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
                                .doOnNext(candidateId -> scores.merge(candidateId, 1, Integer::sum)),
                                REDIS_FANOUT_CONCURRENCY),
                        REDIS_FANOUT_CONCURRENCY)
                .then(Mono.just(scores));
    }

    private Flux<ProductDTO> fillFromCatalogExcluding(Set<UUID> excludeIds, int need) {
        if (need <= 0) {
            return Flux.empty();
        }
        return productRepository.findAll()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .filter(Product::isAvailable)
                .filter(p -> !excludeIds.contains(p.getId()))
                .take(need)
                .map(ProductDTO::from);
    }
}
