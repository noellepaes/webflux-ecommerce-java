package com.ecommerce.order.infrastructure.repository;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final R2dbcOrderEntityRepository orderEntityRepository;
    private final R2dbcOrderItemRepository orderItemRepository;

    @Override
    public Mono<Order> save(Order order) {
        List<OrderItem> items = order.getItems() != null ? new ArrayList<>(order.getItems()) : List.of();
        return orderEntityRepository.save(order)
                .flatMap(saved -> Flux.fromIterable(items)
                        .map(item -> {
                            item.setOrderId(saved.getId());
                            if (item.getId() == null) {
                                item.setId(UUID.randomUUID());
                            }
                            return item;
                        })
                        .flatMap(item -> orderItemRepository.save(item)
                                .doOnNext(OrderItem::markNotNew))
                        .collectList()
                        .defaultIfEmpty(List.of())
                        .map(savedItems -> {
                            saved.setItems(savedItems);
                            return saved;
                        }));
    }

    @Override
    public Mono<Order> findById(UUID id) {
        return orderEntityRepository.findById(id)
                .flatMap(this::attachItems);
    }

    @Override
    public Flux<Order> findByCustomerId(UUID customerId) {
        return orderEntityRepository.findByCustomerId(customerId)
                .flatMap(this::attachItems);
    }

    @Override
    public Flux<Order> findAll() {
        return orderEntityRepository.findAll()
                .flatMap(this::attachItems);
    }

    private Mono<Order> attachItems(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .doOnNext(OrderItem::markNotNew)
                .collectList()
                .defaultIfEmpty(List.of())
                .map(items -> {
                    order.setItems(items);
                    return order;
                });
    }
}
