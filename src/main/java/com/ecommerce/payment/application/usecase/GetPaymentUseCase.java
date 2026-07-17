package com.ecommerce.payment.application.usecase;

import com.ecommerce.payment.application.dto.PaymentDTO;
import com.ecommerce.payment.domain.repository.PaymentRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPaymentUseCase {

    private final PaymentRepository repository;

    @Transactional(readOnly = true)
    public Mono<PaymentDTO> findById(UUID id) {
        return repository.findById(id)
                .map(PaymentDTO::from)
                .switchIfEmpty(Mono.error(new BusinessException("Pagamento não encontrado")));
    }

    @Transactional(readOnly = true)
    public Flux<PaymentDTO> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(PaymentDTO::from);
    }

    @Transactional(readOnly = true)
    public Flux<PaymentDTO> findAll() {
        return repository.findAll().map(PaymentDTO::from);
    }
}
