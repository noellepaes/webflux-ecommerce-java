package com.ecommerce.payment.domain.repository;

import com.ecommerce.payment.domain.model.Payment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentRepository {
    Mono<Payment> save(Payment payment);
    Mono<Payment> findById(UUID id);
    Flux<Payment> findByOrderId(UUID orderId);
    Flux<Payment> findAll();
}
