package com.ecommerce.auth.domain.model;

import com.ecommerce.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "auth_schema", name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    private String email;

    /** Hash BCrypt (salt embutido no próprio hash). */
    @Column("password_hash")
    private String passwordHash;
}
