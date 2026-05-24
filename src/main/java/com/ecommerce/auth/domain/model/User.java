package com.ecommerce.auth.domain.model;

import com.ecommerce.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", schema = "auth_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt (salt embutido no próprio hash). */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;
}
