package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.OrderDTO;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase {

    private final OrderRepository repository;

    public Mono<OrderDTO> findById(UUID id) {
        return repository.findById(id)
                .map(OrderDTO::from)
                .switchIfEmpty(Mono.error(new BusinessException("Pedido não encontrado")));
    }

    public Flux<OrderDTO> findByCustomerId(UUID customerId) {
        return repository.findByCustomerId(customerId).map(OrderDTO::from);
    }

    public Flux<OrderDTO> findAll() {
        return repository.findAll().map(OrderDTO::from);
    }
}
