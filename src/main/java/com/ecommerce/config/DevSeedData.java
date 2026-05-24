package com.ecommerce.config;

import com.ecommerce.auth.application.usecase.RegisterUserUseCase;
import com.ecommerce.customer.application.dto.CustomerDTO;
import com.ecommerce.customer.application.usecase.CreateCustomerUseCase;
import com.ecommerce.customer.domain.model.Customer;
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

import java.math.BigDecimal;


/**
 * Seed DEV: LeBron (esporte), Noelle (criada no banco — views manuais via API), Daniel (tech/games).
 * Visualizações gravadas via {@link ProductViewGraphRedisStore#recordView} — mesmo efeito de
 * {@code POST /api/recommendations/customers/{customerId}/views}.
 */
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
            if (customerRepository.findByEmail(LEBRON_EMAIL).isPresent()) {
                log.info("Seed DEV já existe no banco — sincronizando views no Redis (POST /views)...");
                seedUsers();
                seedRedisViews(loadExistingSeedEntities());
                return;
            }

            log.info("Executando seed DEV: LeBron (esporte), Noelle (teste manual), Daniel (tech/games)...");

            CustomerDTO lebron = createCustomerUseCase.execute(new CustomerDTO(
                    null, "LeBron James", LEBRON_EMAIL, "11111111111", null, null, null
            ));
            CustomerDTO noelle = createCustomerUseCase.execute(new CustomerDTO(
                    null, "Noelle", NOELLE_EMAIL, "22222222222", null, null, null
            ));
            CustomerDTO daniel = createCustomerUseCase.execute(new CustomerDTO(
                    null, "Daniel", DANIEL_EMAIL, "33333333333", null, null, null
            ));

            ProductDTO tenis = createProductUseCase.execute(new ProductDTO(
                    null, TENIS_NAME, "Tênis de corrida amortecido",
                    new BigDecimal("599.00"), 20, null, null, null
            ));
            ProductDTO bola = createProductUseCase.execute(new ProductDTO(
                    null, BOLA_NAME, "Bola oficial tamanho 7",
                    new BigDecimal("189.00"), 30, null, null, null
            ));
            ProductDTO camisa = createProductUseCase.execute(new ProductDTO(
                    null, CAMISA_NAME, "Camisa respirável para treino",
                    new BigDecimal("129.00"), 40, null, null, null
            ));
            ProductDTO notebook = createProductUseCase.execute(new ProductDTO(
                    null, NOTEBOOK_NAME, "Notebook gamer RTX, 16GB RAM",
                    new BigDecimal("4500.00"), 10, null, null, null
            ));
            ProductDTO mouse = createProductUseCase.execute(new ProductDTO(
                    null, MOUSE_NAME, "Mouse gamer 25K DPI",
                    new BigDecimal("450.00"), 25, null, null, null
            ));
            ProductDTO teclado = createProductUseCase.execute(new ProductDTO(
                    null, TECLADO_NAME, "Teclado mecânico switch brown",
                    new BigDecimal("650.00"), 15, null, null, null
            ));
            ProductDTO headset = createProductUseCase.execute(new ProductDTO(
                    null, HEADSET_NAME, "Headset 7.1 surround",
                    new BigDecimal("399.00"), 18, null, null, null
            ));

            seedRedisViews(new SeedEntities(
                    lebron, noelle, daniel,
                    tenis, bola, camisa,
                    notebook, mouse, teclado, headset
            ));

            seedUsers();

            log.info("Seed DEV concluída. Swagger: http://localhost:8080/swagger-ui.html");

        } catch (Exception e) {
            log.error("Erro ao executar seed DEV", e);
        }
    }

    private SeedEntities loadExistingSeedEntities() {
        Customer lebron = customerRepository.findByEmail(LEBRON_EMAIL).orElseThrow();
        Customer noelle = customerRepository.findByEmail(NOELLE_EMAIL).orElseThrow();
        Customer daniel = customerRepository.findByEmail(DANIEL_EMAIL).orElseThrow();

        return new SeedEntities(
                CustomerDTO.from(lebron),
                CustomerDTO.from(noelle),
                CustomerDTO.from(daniel),
                ProductDTO.from(productRepository.findByName(TENIS_NAME).orElseThrow()),
                ProductDTO.from(productRepository.findByName(BOLA_NAME).orElseThrow()),
                ProductDTO.from(productRepository.findByName(CAMISA_NAME).orElseThrow()),
                ProductDTO.from(productRepository.findByName(NOTEBOOK_NAME).orElseThrow()),
                ProductDTO.from(productRepository.findByName(MOUSE_NAME).orElseThrow()),
                ProductDTO.from(productRepository.findByName(TECLADO_NAME).orElseThrow()),
                ProductDTO.from(productRepository.findByName(HEADSET_NAME).orElseThrow())
        );
    }

    /**
     * Simula chamadas a POST /api/recommendations/customers/{id}/views
     */
    private void seedRedisViews(SeedEntities seed) {
        // LeBron — pesquisou/clicou produtos de esporte
        recordView(seed.lebron(), seed.tenis(), "LeBron → esporte");
        recordView(seed.lebron(), seed.bola(), "LeBron → esporte");
        recordView(seed.lebron(), seed.camisa(), "LeBron → esporte");

        // Daniel — perfil games/tech
        recordView(seed.daniel(), seed.notebook(), "Daniel → tech");
        recordView(seed.daniel(), seed.mouse(), "Daniel → tech");
        recordView(seed.daniel(), seed.teclado(), "Daniel → tech");
        recordView(seed.daniel(), seed.headset(), "Daniel → tech");

        log.info("--- Personas seed (IDs para Swagger/curl) ---");
        log.info("LeBron James (esporte): {}", seed.lebron().id());
        log.info("Noelle (teste manual POST /views): {}", seed.noelle().id());
        log.info("Daniel (tech/games): {}", seed.daniel().id());
        log.info("--- Produtos esporte ---");
        log.info("Tênis: {} | Bola: {} | Camisa: {}",
                seed.tenis().id(), seed.bola().id(), seed.camisa().id());
        log.info("--- Produtos tech ---");
        log.info("Notebook: {} | Mouse: {} | Teclado: {} | Headset: {}",
                seed.notebook().id(), seed.mouse().id(), seed.teclado().id(), seed.headset().id());
        log.info("Redis Insight: filtro ecommerce:views:*");
        log.info("Sugestões LeBron: GET /api/recommendations/customers/{}", seed.lebron().id());
        log.info("Noelle — registre views manual: POST /api/recommendations/customers/{}/views", seed.noelle().id());
        log.info("Sugestões Daniel: GET /api/recommendations/customers/{}", seed.daniel().id());
    }

    private void seedUsers() {
        registerUserUseCase.execute(LEBRON_EMAIL, DEV_PASSWORD);
        registerUserUseCase.execute(NOELLE_EMAIL, DEV_PASSWORD);
        registerUserUseCase.execute(DANIEL_EMAIL, DEV_PASSWORD);
        log.info("Usuários DEV (senha '{}'): {}, {}, {}",
                DEV_PASSWORD, LEBRON_EMAIL, NOELLE_EMAIL, DANIEL_EMAIL);
    }

    private void recordView(CustomerDTO customer, ProductDTO product, String context) {
        viewGraphStore.recordView(customer.id(), product.id());
        log.debug("View registrada [{}]: customer={} product={}", context, customer.id(), product.id());
    }

    private record SeedEntities(
            CustomerDTO lebron,
            CustomerDTO noelle,
            CustomerDTO daniel,
            ProductDTO tenis,
            ProductDTO bola,
            ProductDTO camisa,
            ProductDTO notebook,
            ProductDTO mouse,
            ProductDTO teclado,
            ProductDTO headset
    ) {
    }
}
