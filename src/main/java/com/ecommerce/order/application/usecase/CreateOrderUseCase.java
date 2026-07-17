package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.OrderDTO;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository repository;

    @Transactional
    public Mono<OrderDTO> execute(UUID customerId) {
        Order order = new Order();
        order.setCustomerId(customerId);
        return repository.save(order).map(OrderDTO::from);
    }
}
