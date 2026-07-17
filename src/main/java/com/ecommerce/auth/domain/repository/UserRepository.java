package com.ecommerce.auth.domain.repository;

import com.ecommerce.auth.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository {

    Mono<User> save(User user);

    Flux<User> findAll();

    Mono<User> findByEmail(String email);

    Mono<User> findById(UUID id);

    Mono<Boolean> existsByEmail(String email);
}
