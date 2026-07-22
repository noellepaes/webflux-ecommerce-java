package com.ecommerce.order.infrastructure.repository;

import com.ecommerce.order.domain.model.OrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, UUID> {
    Flux<OrderItem> findByOrderId(UUID orderId);

    Flux<OrderItem> findByOrderIdIn(Collection<UUID> orderIds);
}
