package com.ecommerce.auth.application.usecase;

import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Mono<Void> execute(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        return userRepository.existsByEmail(normalizedEmail)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.empty();
                    }
                    return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(hash -> {
                                User user = new User();
                                user.setEmail(normalizedEmail);
                                user.setPasswordHash(hash);
                                return userRepository.save(user);
                            })
                            .then();
                });
    }
}
