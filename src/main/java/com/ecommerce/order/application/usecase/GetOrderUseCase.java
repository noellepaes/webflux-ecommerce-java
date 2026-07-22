package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.OrderDTO;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.infrastructure.repository.OrderItemRepository;
import com.ecommerce.order.infrastructure.repository.OrderRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;

    public Mono<OrderDTO> findById(UUID id) {
        return orderRepository.findById(id)
                .flatMap(this::withItems)
                .map(OrderDTO::from)
                .switchIfEmpty(Mono.error(new BusinessException("Pedido não encontrado")));
    }

    public Flux<OrderDTO> findByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerId(customerId)
                .collectList()
                .flatMapMany(this::withItemsBatch)
                .map(OrderDTO::from);
    }

    public Flux<OrderDTO> findAll() {
        return orderRepository.findAll()
                .collectList()
                .flatMapMany(this::withItemsBatch)
                .map(OrderDTO::from);
    }

    private Mono<Order> withItems(Order order) {
        return itemRepository.findByOrderId(order.getId())
                .doOnNext(OrderItem::markNotNew)
                .collectList()
                .defaultIfEmpty(List.of())
                .map(items -> {
                    order.setItems(items);
                    return order;
                });
    }

    /** 1 query de orders + 1 query items WHERE order_id IN (...). */
    private Flux<Order> withItemsBatch(List<Order> orders) {
        if (orders.isEmpty()) {
            return Flux.empty();
        }
        List<UUID> ids = orders.stream().map(Order::getId).toList();
        return itemRepository.findByOrderIdIn(ids)
                .doOnNext(OrderItem::markNotNew)
                .collectList()
                .defaultIfEmpty(List.of())
                .flatMapMany(items -> {
                    Map<UUID, List<OrderItem>> byOrder = items.stream()
                            .collect(Collectors.groupingBy(OrderItem::getOrderId));
                    for (Order order : orders) {
                        order.setItems(new ArrayList<>(byOrder.getOrDefault(order.getId(), List.of())));
                    }
                    return Flux.fromIterable(orders);
                });
    }
}
