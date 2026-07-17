package com.ecommerce.payment.presentation;

import com.ecommerce.payment.application.dto.PaymentDTO;
import com.ecommerce.payment.application.usecase.GetPaymentUseCase;
import com.ecommerce.payment.application.usecase.ProcessPaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "API para processamento de pagamentos")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    @PostMapping
    @Operation(summary = "Processar pagamento", description = "Processa um pagamento para um pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pagamento processado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou pagamento falhou")
    })
    public Mono<ResponseEntity<Void>> pay(@Valid @RequestBody PaymentRequest request) {
        return processPaymentUseCase.execute(request.orderId(), request.amount(), request.method())
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).<Void>build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pagamento por ID", description = "Retorna um pagamento específico pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    })
    public Mono<ResponseEntity<PaymentDTO>> getPayment(
            @Parameter(description = "ID do pagamento", required = true) @PathVariable UUID id) {
        return getPaymentUseCase.findById(id).map(ResponseEntity::ok);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Listar pagamentos por pedido", description = "Retorna todos os pagamentos de um pedido específico")
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso")
    public Flux<PaymentDTO> getPaymentsByOrder(
            @Parameter(description = "ID do pedido", required = true) @PathVariable UUID orderId) {
        return getPaymentUseCase.findByOrderId(orderId);
    }
}
