package com.ecommerce.order.domain.model;

import com.ecommerce.order.domain.exception.OrderException;
import com.ecommerce.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(schema = "order_schema", name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Column("customer_id")
    private UUID customerId;

    @Transient
    private List<OrderItem> items = new ArrayList<>();

    private OrderStatus status = OrderStatus.PENDING;

    @Column("total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public void addItem(OrderItem item) {
        item.setOrderId(this.getId());
        this.items.add(item);
        calculateTotal();
    }

    public void removeItem(OrderItem item) {
        this.items.remove(item);
        item.setOrderId(null);
        calculateTotal();
    }

    private void calculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderException(
                String.format("Pedido não pode ser pago. Status atual: %s. Apenas pedidos PENDING podem ser pagos.", this.status)
            );
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (this.status == OrderStatus.PAID) {
            throw new OrderException("Pedido pago não pode ser cancelado");
        }
        if (this.status == OrderStatus.CANCELLED) {
            throw new OrderException("Pedido já está cancelado");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderException("Apenas pedidos PENDING podem ser confirmados");
        }
        this.status = OrderStatus.CONFIRMED;
    }
}
