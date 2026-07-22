package com.ecommerce.order.infrastructure.repository;

import com.ecommerce.order.domain.model.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, UUID> {
    Flux<Order> findByCustomerId(UUID customerId);
}
