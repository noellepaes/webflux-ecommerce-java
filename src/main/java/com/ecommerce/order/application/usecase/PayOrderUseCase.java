package com.ecommerce.order.application.usecase;

import com.ecommerce.order.application.dto.OrderDTO;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayOrderUseCase {

    private final OrderRepository repository;

    @Transactional
    public Mono<OrderDTO> execute(UUID orderId) {
        return repository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException("Pedido não encontrado")))
                .flatMap(order -> {
                    order.pay();
                    return repository.save(order);
                })
                .map(OrderDTO::from);
    }
}
