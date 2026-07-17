package com.ecommerce.auth.infrastructure.repository;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.repository.UserRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface R2dbcUserRepository extends ReactiveCrudRepository<User, UUID>, UserRepository {

    @Override
    Mono<User> findByEmail(String email);

    @Override
    Mono<Boolean> existsByEmail(String email);
}
