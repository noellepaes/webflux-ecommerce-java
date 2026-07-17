package com.ecommerce.payment.application.usecase;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentMethod;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.repository.PaymentRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository repository;

    @Transactional
    public Mono<Void> execute(UUID orderId, BigDecimal amount, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setMethod(method);
        processDirectly(payment);
        return repository.save(payment).then();
    }

    private void processDirectly(Payment payment) {
        log.info("Processando pagamento {} (modo simples, sem adapter)", payment.getId());

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Pagamento já processado");
        }

        boolean success = payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0;

        if (success) {
            payment.setStatus(PaymentStatus.APPROVED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
        }
    }
}
