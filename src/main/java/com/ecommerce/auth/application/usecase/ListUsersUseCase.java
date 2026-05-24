package com.ecommerce.auth.application.usecase;

import com.ecommerce.auth.application.dto.UserSummaryDTO;
import com.ecommerce.auth.domain.repository.UserRepository;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<UserSummaryDTO> execute() {
        return userRepository.findAll().stream()
                .map(user -> customerRepository.findByEmail(user.getEmail())
                        .map(customer -> new UserSummaryDTO(user.getEmail(), customer.getName()))
                        .orElse(new UserSummaryDTO(user.getEmail(), user.getEmail())))
                .toList();
    }
}
