package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrderRepository {
    Mono<Order> save(Order order);
    Mono<Order> findById(UUID id);
    Flux<Order> findByCustomerId(UUID customerId);
    Flux<Order> findAll();
}
