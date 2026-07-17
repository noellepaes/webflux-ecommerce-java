package com.ecommerce.customer.application.usecase;

import com.ecommerce.customer.application.dto.CustomerDTO;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCustomerUseCase {

    private final CustomerRepository repository;

    @Transactional(readOnly = true)
    public Mono<CustomerDTO> findById(UUID id) {
        return repository.findById(id)
                .map(CustomerDTO::from)
                .switchIfEmpty(Mono.error(new BusinessException("Cliente não encontrado")));
    }

    @Transactional(readOnly = true)
    public Flux<CustomerDTO> findAll() {
        return repository.findAll().map(CustomerDTO::from);
    }
}
