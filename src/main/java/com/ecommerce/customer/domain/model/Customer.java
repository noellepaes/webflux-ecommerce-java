package com.ecommerce.customer.domain.model;

import com.ecommerce.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "customer_schema", name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    private String name;
    private String email;
    private String cpf;
    private CustomerStatus status = CustomerStatus.ACTIVE;

    public void activate() {
        this.status = CustomerStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }
}
