package com.ecommerce.auth.application.usecase;

import com.ecommerce.auth.application.dto.LoginResponse;
import com.ecommerce.auth.domain.repository.UserRepository;
import com.ecommerce.auth.presentation.LoginRequest;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.shared.exception.BusinessException;
import com.ecommerce.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<LoginResponse> execute(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Email ou senha inválidos")))
                .flatMap(user -> Mono.fromCallable(() -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(matches -> {
                            if (!matches) {
                                return Mono.error(new UnauthorizedException("Email ou senha inválidos"));
                            }
                            return customerRepository.findByEmail(user.getEmail());
                        }))
                .switchIfEmpty(Mono.error(new BusinessException("Cliente não encontrado para este usuário")))
                .flatMap(customer -> {
                    if (!customer.isActive()) {
                        return Mono.error(new UnauthorizedException("Conta desativada"));
                    }
                    return Mono.just(new LoginResponse(customer.getId(), customer.getName(), customer.getEmail()));
                });
    }
}
