package com.ecommerce.payment.domain.model;

import com.ecommerce.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table(schema = "payment_schema", name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Column("order_id")
    private UUID orderId;

    private BigDecimal amount;
    private PaymentStatus status = PaymentStatus.PENDING;
    private PaymentMethod method;
}
