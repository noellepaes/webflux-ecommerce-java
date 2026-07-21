package com.ecommerce.payment.infrastructure.repository;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final R2dbcPaymentRepository delegate;

    @Override
    public Mono<Payment> save(Payment payment) {
        return delegate.save(payment);
    }

    @Override
    public Mono<Payment> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public Flux<Payment> findByOrderId(UUID orderId) {
        return delegate.findByOrderId(orderId);
    }

    @Override
    public Flux<Payment> findAll() {
        return delegate.findAll();
    }
}
