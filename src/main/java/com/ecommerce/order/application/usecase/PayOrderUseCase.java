package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.OrderDTO;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.infrastructure.repository.OrderItemRepository;
import com.ecommerce.order.infrastructure.repository.OrderRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;

    @Transactional
    public Mono<OrderDTO> execute(UUID orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException("Pedido não encontrado")))
                .flatMap(order -> itemRepository.findByOrderId(orderId)
                        .doOnNext(OrderItem::markNotNew)
                        .collectList()
                        .defaultIfEmpty(List.of())
                        .flatMap(items -> {
                            order.setItems(items);
                            order.pay();
                            return orderRepository.save(order);
                        }))
                .map(OrderDTO::from);
    }
}
