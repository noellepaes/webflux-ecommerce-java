package com.ecommerce.customer.infrastructure.repository;

import com.ecommerce.customer.domain.model.Customer;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final R2dbcCustomerRepository delegate;

    @Override
    public Mono<Customer> save(Customer customer) {
        return delegate.save(customer);
    }

    @Override
    public Mono<Customer> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public Mono<Customer> findByEmail(String email) {
        return delegate.findByEmail(email);
    }

    @Override
    public Flux<Customer> findByEmailIn(Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Flux.empty();
        }
        return delegate.findByEmailIn(emails);
    }

    @Override
    public Mono<Customer> findByCpf(String cpf) {
        return delegate.findByCpf(cpf);
    }

    @Override
    public Flux<Customer> findAll() {
        return delegate.findAll();
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return delegate.deleteById(id);
    }
}
