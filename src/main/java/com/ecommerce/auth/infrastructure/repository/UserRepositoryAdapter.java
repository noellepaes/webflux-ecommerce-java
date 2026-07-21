package com.ecommerce.auth.infrastructure.repository;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final R2dbcUserRepository delegate;

    @Override
    public Mono<User> save(User user) {
        return delegate.save(user);
    }

    @Override
    public Flux<User> findAll() {
        return delegate.findAll();
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return delegate.findByEmail(email);
    }

    @Override
    public Mono<User> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return delegate.existsByEmail(email);
    }
}
