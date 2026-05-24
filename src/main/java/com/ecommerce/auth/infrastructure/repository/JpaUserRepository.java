package com.ecommerce.auth.infrastructure.repository;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {

    @Override
    Optional<User> findByEmail(String email);

    @Override
    boolean existsByEmail(String email);
}
