package com.ecommerce.customer.application.usecase;

import com.ecommerce.customer.application.dto.CustomerDTO;
import com.ecommerce.customer.domain.model.Customer;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateCustomerUseCase {

    private final CustomerRepository repository;

    @Transactional
    public Mono<CustomerDTO> execute(CustomerDTO customerDTO) {
        return repository.findByEmail(customerDTO.email()).hasElement()
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new BusinessException("Cliente com este email já existe"));
                    }
                    return repository.findByCpf(customerDTO.cpf()).hasElement();
                })
                .flatMap(cpfExists -> {
                    if (Boolean.TRUE.equals(cpfExists)) {
                        return Mono.error(new BusinessException("Cliente com este CPF já existe"));
                    }
                    Customer customer = new Customer();
                    customer.setName(customerDTO.name());
                    customer.setEmail(customerDTO.email());
                    customer.setCpf(customerDTO.cpf());
                    return repository.save(customer);
                })
                .map(CustomerDTO::from);
    }
}
