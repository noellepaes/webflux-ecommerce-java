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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddItemToOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;

    @Transactional
    public Mono<OrderDTO> execute(UUID orderId, UUID productId, String productName, Integer quantity, BigDecimal unitPrice) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException("Pedido não encontrado")))
                .flatMap(order -> itemRepository.findByOrderId(orderId)
                        .doOnNext(OrderItem::markNotNew)
                        .collectList()
                        .defaultIfEmpty(List.of())
                        .flatMap(existing -> {
                            order.setItems(new ArrayList<>(existing));

                            OrderItem item = new OrderItem();
                            item.setId(UUID.randomUUID());
                            item.setProductId(productId);
                            item.setProductName(productName);
                            item.setQuantity(quantity);
                            item.setUnitPrice(unitPrice);
                            order.addItem(item);

                            return orderRepository.save(order)
                                    .flatMap(saved -> itemRepository.save(item)
                                            .doOnNext(OrderItem::markNotNew)
                                            .thenReturn(saved));
                        }))
                .map(OrderDTO::from);
    }
}
