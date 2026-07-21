package com.ecommerce.order.application.dto;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record OrderDTO(
        UUID id,
        UUID customerId,
        List<OrderItemDTO> items,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderDTO from(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getCustomerId(),
                order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(OrderItemDTO::from)
                        .collect(Collectors.toList()),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
