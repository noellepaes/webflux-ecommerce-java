package com.ecommerce.auth.application.usecase;

import com.ecommerce.auth.application.dto.LoginResponse;
import com.ecommerce.auth.domain.repository.UserRepository;
import com.ecommerce.auth.presentation.LoginRequest;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ecommerce.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse execute(LoginRequest request) {
        var user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email ou senha inválidos");
        }

        var customer = customerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado para este usuário"));

        if (!customer.isActive()) {
            throw new UnauthorizedException("Conta desativada");
        }

        return new LoginResponse(customer.getId(), customer.getName(), customer.getEmail());
    }
}
