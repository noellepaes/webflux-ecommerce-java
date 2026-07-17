package com.ecommerce.customer.application.usecase;

import com.ecommerce.customer.application.dto.CustomerDTO;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeactivateCustomerUseCase {

    private final CustomerRepository repository;

    @Transactional
    public Mono<CustomerDTO> execute(UUID id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Cliente não encontrado")))
                .flatMap(customer -> {
                    customer.deactivate();
                    return repository.save(customer);
                })
                .map(CustomerDTO::from);
    }
}
