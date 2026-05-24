package com.ecommerce.auth.presentation;

import com.ecommerce.auth.application.dto.LoginResponse;
import com.ecommerce.auth.application.dto.UserSummaryDTO;
import com.ecommerce.auth.application.usecase.ListUsersUseCase;
import com.ecommerce.auth.application.usecase.LoginUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login simples com email e senha (BCrypt)")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final ListUsersUseCase listUsersUseCase;

    @GetMapping("/users")
    @Operation(summary = "Listar usuários", description = "Retorna emails e nomes dos usuários cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de usuários")
    public ResponseEntity<List<UserSummaryDTO>> listUsers() {
        return ResponseEntity.ok(listUsersUseCase.execute());
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Valida credenciais e retorna o customerId vinculado ao email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }
}
