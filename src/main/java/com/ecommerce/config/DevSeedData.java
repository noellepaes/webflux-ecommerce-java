package com.ecommerce.config;

import com.ecommerce.auth.application.usecase.RegisterUserUseCase;
import com.ecommerce.customer.application.dto.CustomerDTO;
import com.ecommerce.customer.application.usecase.CreateCustomerUseCase;
import com.ecommerce.customer.domain.repository.CustomerRepository;
import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.application.usecase.CreateProductUseCase;
import com.ecommerce.product.domain.repository.ProductRepository;
import com.ecommerce.recommendation.infrastructure.ProductViewGraphRedisStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DevSeedData implements CommandLineRunner {

    private static final String LEBRON_EMAIL = "lebron.seed@dev.local";
    private static final String NOELLE_EMAIL = "noelle.seed@dev.local";
    private static final String DANIEL_EMAIL = "daniel.seed@dev.local";
    private static final String DEV_PASSWORD = "123456";

    private static final String TENIS_NAME = "Tênis Nike Air Max";
    private static final String BOLA_NAME = "Bola de Basquete Spalding";
    private static final String CAMISA_NAME = "Camisa Esportiva Dry-Fit";
    private static final String NOTEBOOK_NAME = "Notebook Gamer RTX";
    private static final String MOUSE_NAME = "Mouse Logitech G Pro";
    private static final String TECLADO_NAME = "Teclado Mecânico RGB";
    private static final String HEADSET_NAME = "Headset Gamer HyperX";

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CreateCustomerUseCase createCustomerUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final ProductViewGraphRedisStore viewGraphStore;
    private final RegisterUserUseCase registerUserUseCase;

    @Override
    public void run(String... args) {
        try {
            seed().block(Duration.ofMinutes(2));
        } catch (Exception e) {
            log.error("Erro ao executar seed DEV", e);
        }
    }

    private Mono<Void> seed() {
        return customerRepository.findByEmail(LEBRON_EMAIL)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Seed DEV já existe no banco — sincronizando views no Redis...");
                        return seedUsers()
                                .then(loadExistingSeedEntities())
                                .flatMap(this::seedRedisViews);
                    }
                    log.info("Executando seed DEV: LeBron (esporte), Noelle (teste manual), Daniel (tech/games)...");
                    return createFreshSeed();
                });
    }

    private Mono<Void> createFreshSeed() {
        Mono<CustomerDTO> lebron = createCustomerUseCase.execute(new CustomerDTO(
                null, "LeBron James", LEBRON_EMAIL, "11111111111", null, null, null));
        Mono<CustomerDTO> noelle = createCustomerUseCase.execute(new CustomerDTO(
                null, "Noelle", NOELLE_EMAIL, "22222222222", null, null, null));
        Mono<CustomerDTO> daniel = createCustomerUseCase.execute(new CustomerDTO(
                null, "Daniel", DANIEL_EMAIL, "33333333333", null, null, null));

        Mono<ProductDTO> tenis = createProduct("Tênis de corrida amortecido", TENIS_NAME, "599.00", 20);
        Mono<ProductDTO> bola = createProduct("Bola oficial tamanho 7", BOLA_NAME, "189.00", 30);
        Mono<ProductDTO> camisa = createProduct("Camisa respirável para treino", CAMISA_NAME, "129.00", 40);
        Mono<ProductDTO> notebook = createProduct("Notebook gamer RTX, 16GB RAM", NOTEBOOK_NAME, "4500.00", 10);
        Mono<ProductDTO> mouse = createProduct("Mouse gamer 25K DPI", MOUSE_NAME, "450.00", 25);
        Mono<ProductDTO> teclado = createProduct("Teclado mecânico switch brown", TECLADO_NAME, "650.00", 15);
        Mono<ProductDTO> headset = createProduct("Headset 7.1 surround", HEADSET_NAME, "399.00", 18);

        return Mono.zip(lebron, noelle, daniel)
                .flatMap(customers -> Mono.zip(tenis, bola, camisa, notebook, mouse, teclado, headset)
                        .map(products -> new SeedEntities(
                                customers.getT1(), customers.getT2(), customers.getT3(),
                                products.getT1(), products.getT2(), products.getT3(),
                                products.getT4(), products.getT5(), products.getT6(), products.getT7()))
                        .flatMap(seed -> seedRedisViews(seed).then(seedUsers())));
    }

    private Mono<ProductDTO> createProduct(String description, String name, String price, int stock) {
        return createProductUseCase.execute(new ProductDTO(
                null, name, description, new BigDecimal(price), stock, null, null, null));
    }

    private Mono<SeedEntities> loadExistingSeedEntities() {
        return Mono.zip(
                        customerRepository.findByEmail(LEBRON_EMAIL).map(CustomerDTO::from),
                        customerRepository.findByEmail(NOELLE_EMAIL).map(CustomerDTO::from),
                        customerRepository.findByEmail(DANIEL_EMAIL).map(CustomerDTO::from)
                )
                .flatMap(customers -> Mono.zip(
                                productRepository.findByName(TENIS_NAME).map(ProductDTO::from),
                                productRepository.findByName(BOLA_NAME).map(ProductDTO::from),
                                productRepository.findByName(CAMISA_NAME).map(ProductDTO::from),
                                productRepository.findByName(NOTEBOOK_NAME).map(ProductDTO::from),
                                productRepository.findByName(MOUSE_NAME).map(ProductDTO::from),
                                productRepository.findByName(TECLADO_NAME).map(ProductDTO::from),
                                productRepository.findByName(HEADSET_NAME).map(ProductDTO::from)
                        )
                        .map(products -> new SeedEntities(
                                customers.getT1(), customers.getT2(), customers.getT3(),
                                products.getT1(), products.getT2(), products.getT3(),
                                products.getT4(), products.getT5(), products.getT6(), products.getT7())));
    }

    private Mono<Void> seedRedisViews(SeedEntities seed) {
        return viewGraphStore.recordView(seed.lebron().id(), seed.tenis().id())
                .then(viewGraphStore.recordView(seed.lebron().id(), seed.bola().id()))
                .then(viewGraphStore.recordView(seed.lebron().id(), seed.camisa().id()))
                .then(viewGraphStore.recordView(seed.daniel().id(), seed.notebook().id()))
                .then(viewGraphStore.recordView(seed.daniel().id(), seed.mouse().id()))
                .then(viewGraphStore.recordView(seed.daniel().id(), seed.teclado().id()))
                .then(viewGraphStore.recordView(seed.daniel().id(), seed.headset().id()))
                .doOnSuccess(v -> {
                    log.info("--- Personas seed (IDs para Swagger/curl) ---");
                    log.info("LeBron James (esporte): {}", seed.lebron().id());
                    log.info("Noelle (teste manual POST /views): {}", seed.noelle().id());
                    log.info("Daniel (tech/games): {}", seed.daniel().id());
                    log.info("Seed DEV concluída. Swagger: http://localhost:8080/swagger-ui.html");
                });
    }

    private Mono<Void> seedUsers() {
        return registerUserUseCase.execute(LEBRON_EMAIL, DEV_PASSWORD)
                .then(registerUserUseCase.execute(NOELLE_EMAIL, DEV_PASSWORD))
                .then(registerUserUseCase.execute(DANIEL_EMAIL, DEV_PASSWORD))
                .doOnSuccess(v -> log.info("Usuários DEV (senha '{}'): {}, {}, {}",
                        DEV_PASSWORD, LEBRON_EMAIL, NOELLE_EMAIL, DANIEL_EMAIL));
    }

    private record SeedEntities(
            CustomerDTO lebron, CustomerDTO noelle, CustomerDTO daniel,
            ProductDTO tenis, ProductDTO bola, ProductDTO camisa,
            ProductDTO notebook, ProductDTO mouse, ProductDTO teclado, ProductDTO headset
    ) {
    }
}
