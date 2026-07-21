package com.ecommerce.auth.application.usecase;

import com.ecommerce.auth.application.dto.UserSummaryDTO;
import com.ecommerce.auth.domain.model.User;
import com.ecommerce.auth.domain.repository.UserRepository;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    /**
     * Lista users com nome do customer em 2 queries (users + customers IN emails),
     * evitando N+1 ({@code findAll} + {@code findByEmail} por usuário).
     */
    @Transactional(readOnly = true)
    public Flux<UserSummaryDTO> execute() {
        return userRepository.findAll()
                .collectList()
                .flatMapMany(users -> {
                    if (users.isEmpty()) {
                        return Flux.empty();
                    }
                    var emails = users.stream().map(User::getEmail).toList();
                    return customerRepository.findByEmailIn(emails)
                            .collect(Collectors.toMap(
                                    c -> c.getEmail().toLowerCase(Locale.ROOT),
                                    Function.identity(),
                                    (a, b) -> a))
                            .flatMapMany(customersByEmail -> Flux.fromIterable(users)
                                    .map(user -> {
                                        var customer = customersByEmail.get(
                                                user.getEmail().toLowerCase(Locale.ROOT));
                                        String name = customer != null ? customer.getName() : user.getEmail();
                                        return new UserSummaryDTO(user.getEmail(), name);
                                    }));
                });
    }
}
