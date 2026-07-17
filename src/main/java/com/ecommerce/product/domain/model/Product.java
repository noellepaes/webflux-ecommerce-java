package com.ecommerce.product.domain.model;

import com.ecommerce.product.domain.exception.ProductException;
import com.ecommerce.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table(schema = "product_schema", name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status = ProductStatus.ACTIVE;

    public void decreaseStock(Integer quantity) {
        if (quantity <= 0) {
            throw new ProductException("Quantidade deve ser maior que zero");
        }
        if (this.stock < quantity) {
            throw new ProductException("Estoque insuficiente");
        }
        this.stock -= quantity;
    }

    public void increaseStock(Integer quantity) {
        if (quantity <= 0) {
            throw new ProductException("Quantidade deve ser maior que zero");
        }
        this.stock += quantity;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && stock > 0;
    }
}
