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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * R2DBC não tem {@code @EntityGraph}. O equivalente para listagens é:
 * 1 query de orders + 1 query {@code items WHERE order_id IN (...)}.
 */
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
                .collectList()
                .flatMapMany(this::attachItemsBatch);
    }

    @Override
    public Flux<Order> findAll() {
        return orderEntityRepository.findAll()
                .collectList()
                .flatMapMany(this::attachItemsBatch);
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

    private Flux<Order> attachItemsBatch(List<Order> orders) {
        if (orders.isEmpty()) {
            return Flux.empty();
        }
        List<UUID> orderIds = orders.stream().map(Order::getId).toList();
        final int chunkSize = 500;

        return Flux.fromIterable(orderIds)
                .buffer(chunkSize)
                .concatMap(chunk -> orderItemRepository.findByOrderIdIn(chunk)
                        .doOnNext(OrderItem::markNotNew))
                .collectList()
                .defaultIfEmpty(List.of())
                .flatMapMany(items -> {
                    Map<UUID, List<OrderItem>> byOrder = items.stream()
                            .collect(Collectors.groupingBy(OrderItem::getOrderId));
                    for (Order order : orders) {
                        List<OrderItem> orderItems = byOrder.getOrDefault(order.getId(), List.of());
                        order.setItems(new ArrayList<>(orderItems));
                    }
                    return Flux.fromIterable(orders);
                });
    }
}
