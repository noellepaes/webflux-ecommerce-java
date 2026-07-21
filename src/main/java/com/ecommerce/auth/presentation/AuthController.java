package com.ecommerce.auth.presentation;

import com.ecommerce.auth.application.dto.UserSummaryDTO;
import com.ecommerce.auth.application.usecase.ListUsersUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Listagem de usuários (login adiado — bcrypt/CPU-bound)")
public class AuthController {

    private final ListUsersUseCase listUsersUseCase;

    @GetMapping("/users")
    @Operation(summary = "Listar usuários", description = "Retorna emails e nomes dos usuários cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de usuários")
    public Mono<List<UserSummaryDTO>> listUsers() {
        return listUsersUseCase.execute().collectList();
    }
}
