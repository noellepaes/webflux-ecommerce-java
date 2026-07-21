package com.ecommerce.product.presentation;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.application.usecase.CreateProductUseCase;
import com.ecommerce.product.application.usecase.DecreaseStockUseCase;
import com.ecommerce.product.application.usecase.UpdateProductUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Mutations de produto. Reads ({@code GET /api/products}, {@code GET /api/products/{id}})
 * ficam no roteamento funcional ({@link ProductRouterConfig} + {@link ProductReadHandler}).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "API para gerenciamento de produtos (writes; reads via RouterFunction)")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DecreaseStockUseCase decreaseStockUseCase;

    @PostMapping
    @Operation(summary = "Criar produto", description = "Cria um novo produto no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public Mono<ResponseEntity<ProductDTO>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductDTO productDTO = new ProductDTO(
                null, request.name(), request.description(),
                request.price(), request.stock(), null, null, null
        );
        return createProductUseCase.execute(productDTO)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto", description = "Atualiza os dados de um produto existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public Mono<ResponseEntity<ProductDTO>> updateProduct(
            @Parameter(description = "ID do produto", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        ProductDTO productDTO = new ProductDTO(
                null, request.name(), request.description(),
                request.price(), null, null, null, null
        );
        return updateProductUseCase.execute(id, productDTO).map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/decrease-stock")
    @Operation(summary = "Diminuir estoque", description = "Reduz a quantidade em estoque de um produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estoque atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou estoque insuficiente"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    public Mono<ResponseEntity<ProductDTO>> decreaseStock(
            @Parameter(description = "ID do produto", required = true) @PathVariable UUID id,
            @Parameter(description = "Quantidade a diminuir", required = true) @RequestParam Integer quantity) {
        return decreaseStockUseCase.execute(id, quantity).map(ResponseEntity::ok);
    }
}
