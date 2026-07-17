package com.ecommerce.recommendation.infrastructure;

import com.ecommerce.recommendation.config.RecommendationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Associação usuário ↔ produto só no Redis (sem entidade R2DBC):
 * <ul>
 *   <li>{@code ecommerce:views:user:{customerId}} — SET de productIds que o cliente viu/clicou</li>
 *   <li>{@code ecommerce:views:product:{productId}} — SET de customerIds que viram o produto</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProductViewGraphRedisStore {

    private static final String USER_VIEWS_PREFIX = "ecommerce:views:user:";
    private static final String PRODUCT_VIEWERS_PREFIX = "ecommerce:views:product:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RecommendationProperties recommendationProperties;

    public Mono<Void> recordView(UUID customerId, UUID productId) {
        String userKey = userViewsKey(customerId);
        String productKey = productViewersKey(productId);
        Duration ttl = recommendationProperties.customerHistoryTtl();

        return redisTemplate.opsForSet().add(userKey, productId.toString())
                .then(redisTemplate.opsForSet().add(productKey, customerId.toString()))
                .then(redisTemplate.expire(userKey, ttl))
                .then(redisTemplate.expire(productKey, ttl))
                .then();
    }

    public Flux<UUID> getUserViewedProductIds(UUID customerId) {
        return redisTemplate.opsForSet().members(userViewsKey(customerId))
                .map(UUID::fromString);
    }

    public Flux<UUID> getProductViewerIds(UUID productId) {
        return redisTemplate.opsForSet().members(productViewersKey(productId))
                .map(UUID::fromString);
    }

    private static String userViewsKey(UUID customerId) {
        return USER_VIEWS_PREFIX + customerId;
    }

    private static String productViewersKey(UUID productId) {
        return PRODUCT_VIEWERS_PREFIX + productId;
    }
}
