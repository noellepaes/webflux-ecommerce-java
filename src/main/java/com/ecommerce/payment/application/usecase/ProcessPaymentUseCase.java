package com.ecommerce.payment.application.usecase;

import com.ecommerce.payment.domain.model.Payment;
import com.ecommerce.payment.domain.model.PaymentMethod;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.infrastructure.repository.JpaPaymentRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessPaymentUseCase {
    
    private final JpaPaymentRepository repository;
    
    @Transactional
    public void execute(UUID orderId, BigDecimal amount, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setMethod(method);

        processDirectly(payment);

        repository.save(payment);
    }

    /**
     * Etapa 1 (simples): processamento direto aqui no use case.
     * Etapa 2 (quando crescer): extrair para um adapter (ex: PaymentProcessor + Stripe/PayPal).
     */
    private void processDirectly(Payment payment) {
        log.info("Processando pagamento {} (modo simples, sem adapter)", payment.getId());

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Pagamento já processado");
        }

        // Simulação bem simples: aprova pagamentos com valor > 0, senão falha.
        // Se amanhã você integrar Stripe/PayPal de verdade, esse trecho vira um adapter.
        boolean success = payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0;

        if (success) {
            payment.setStatus(PaymentStatus.APPROVED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        // Só pra deixar explícito: após processar, o status não deve ficar PENDING
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
        }
    }
}
