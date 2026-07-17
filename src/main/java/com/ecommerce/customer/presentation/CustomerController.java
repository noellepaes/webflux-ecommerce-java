package com.ecommerce.customer.presentation;

import com.ecommerce.customer.application.dto.CustomerDTO;
import com.ecommerce.customer.application.usecase.CreateCustomerUseCase;
import com.ecommerce.customer.application.usecase.DeactivateCustomerUseCase;
import com.ecommerce.customer.application.usecase.GetCustomerUseCase;
import com.ecommerce.customer.application.usecase.UpdateCustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "API para gerenciamento de clientes")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeactivateCustomerUseCase deactivateCustomerUseCase;

    @PostMapping
    @Operation(summary = "Criar cliente", description = "Cria um novo cliente no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public Mono<ResponseEntity<CustomerDTO>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerDTO customerDTO = new CustomerDTO(
                null, request.name(), request.email(),
                request.cpf(), null, null, null
        );
        return createCustomerUseCase.execute(customerDTO)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID", description = "Retorna um cliente específico pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public Mono<ResponseEntity<CustomerDTO>> getCustomer(
            @Parameter(description = "ID do cliente", required = true) @PathVariable UUID id) {
        return getCustomerUseCase.findById(id).map(ResponseEntity::ok);
    }

    @GetMapping
    @Operation(summary = "Listar todos os clientes", description = "Retorna uma lista com todos os clientes cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso")
    public Flux<CustomerDTO> getAllCustomers() {
        return getCustomerUseCase.findAll();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente", description = "Atualiza os dados de um cliente existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public Mono<ResponseEntity<CustomerDTO>> updateCustomer(
            @Parameter(description = "ID do cliente", required = true) @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        CustomerDTO customerDTO = new CustomerDTO(
                null, request.name(), request.email(),
                null, null, null, null
        );
        return updateCustomerUseCase.execute(id, customerDTO).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cliente", description = "Desativa um cliente (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public Mono<ResponseEntity<CustomerDTO>> deactivateCustomer(
            @Parameter(description = "ID do cliente", required = true) @PathVariable UUID id) {
        return deactivateCustomerUseCase.execute(id).map(ResponseEntity::ok);
    }
}
