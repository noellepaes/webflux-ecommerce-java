package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.OrderDTO;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase {

    private final OrderRepository repository;

    @Transactional(readOnly = true)
    public Mono<OrderDTO> findById(UUID id) {
        return repository.findById(id)
                .map(OrderDTO::from)
                .switchIfEmpty(Mono.error(new BusinessException("Pedido não encontrado")));
    }

    @Transactional(readOnly = true)
    public Flux<OrderDTO> findByCustomerId(UUID customerId) {
        return repository.findByCustomerId(customerId).map(OrderDTO::from);
    }

    @Transactional(readOnly = true)
    public Flux<OrderDTO> findAll() {
        return repository.findAll().map(OrderDTO::from);
    }
}
