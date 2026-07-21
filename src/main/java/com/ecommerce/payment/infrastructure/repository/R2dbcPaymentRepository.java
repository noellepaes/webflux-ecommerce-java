package com.ecommerce.payment.infrastructure.repository;

import com.ecommerce.payment.domain.model.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface R2dbcPaymentRepository extends ReactiveCrudRepository<Payment, UUID> {
    Flux<Payment> findByOrderId(UUID orderId);
}
