# 🏗️ Arquitetura Completa do Projeto

## 📊 Diagrama de Fluxo Completo

### Exemplo: Criar um Pedido (Order)

```
┌───────────────────────────────────────────────────────────────────┐
│                        CLIENTE (HTTP Request)                     │
│  POST /api/orders                                                 │
│  { "customerId": "uuid-do-cliente" }                              │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                    CAMADA DE APRESENTAÇÃO                         │
│                    (Presentation Layer)                           │
│                                                                   │
│  OrderController                                                  │
│  ├─ Recebe OrderRequest (DTO HTTP)                                │
│  ├─ Valida com @Valid                                             │
│  └─ Chama CreateOrderUseCase.execute()                            │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                    CAMADA DE APLICAÇÃO                            │
│                    (Application Layer)                            │
│                                                                   │
│  CreateOrderUseCase                                               │
│  ├─ Orquestra a criação do pedido                                 │
│  ├─ Cria entidade Order (domínio)                                 │
│  ├─ Salva via OrderRepository.save()                              │
│  └─ Retorna OrderDTO (DTO interno)                                │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    CAMADA DE DOMÍNIO                                │
│                    (Domain Layer)                                   │
│                                                                     │
│  Order (Aggregate Root)                                            │
│  ├─ Regras de negócio:                                             │
│  │  • addItem() - adiciona item e recalcula total                  │
│  │  • pay() - valida transição PENDING → PAID                      │
│  │  • cancel() - valida se pode cancelar                           │
│  ├─ Herda de BaseEntity (shared)                                  │
│  └─ Lança OrderException se regra violada                          │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    CAMADA DE INFRAESTRUTURA                         │
│                    (Infrastructure Layer)                          │
│                                                                     │
│  JpaOrderRepository                                                 │
│  ├─ Implementa OrderRepository (interface do domínio)              │
│  ├─ Estende JpaRepository (Spring Data JPA)                       │
│  └─ Persiste no banco PostgreSQL (order_schema.orders)            │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         BANCO DE DADOS                              │
│                    PostgreSQL (order_schema)                        │
│                                                                     │
│  orders table                                                       │
│  ├─ id (UUID)                                                      │
│  ├─ customer_id (UUID)                                             │
│  ├─ status (VARCHAR)                                               │
│  ├─ total_amount (NUMERIC)                                         │
│  ├─ created_at (TIMESTAMP)                                        │
│  ├─ updated_at (TIMESTAMP)                                         │
│  └─ version (BIGINT) - Optimistic Locking                          │
└─────────────────────────────────────────────────────────────────────┘
```

## 🎯 Arquitetura em Camadas (DDD)

```
┌─────────────────────────────────────────────────────────────────┐
│                         PRESENTATION                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Product    │  │   Customer   │  │    Order     │         │
│  │  Controller  │  │  Controller  │  │  Controller  │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│         │                 │                 │                   │
└─────────┼─────────────────┼─────────────────┼───────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                         APPLICATION                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Product    │  │   Customer   │  │    Order     │         │
│  │   UseCases   │  │   UseCases  │  │   UseCases  │         │
│  │              │  │             │  │             │         │
│  │  • Create    │  │  • Create   │  │  • Create   │         │
│  │  • Get        │  │  • Get      │  │  • AddItem  │         │
│  │  • Update     │  │  • Update   │  │  • Pay      │         │
│  │  • Decrease   │  │  • Deactivate│ │  • Cancel   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│         │                 │                 │                   │
└─────────┼─────────────────┼─────────────────┼───────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                           DOMAIN                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Product    │  │   Customer   │  │    Order     │         │
│  │              │  │              │  │              │         │
│  │  • Model     │  │  • Model     │  │  • Model     │         │
│  │  • Repository │  │  • Repository│  │  • Repository│         │
│  │  • Exception  │  │  • Exception │  │  • Exception │         │
│  │              │  │              │  │              │         │
│  │  Regras:     │  │  Regras:     │  │  Regras:     │         │
│  │  • decrease  │  │  • activate  │  │  • pay()     │         │
│  │    Stock()   │  │  • deactivate│  │  • cancel()  │         │
│  │  • isAvail() │  │  • isActive()│  │  • addItem() │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                 │                 │                   │
│         └─────────────────┼─────────────────┘                   │
│                           │                                     │
│                           ▼                                     │
│              ┌───────────────────────┐                          │
│              │    SHARED (Base)      │                          │
│              │                       │                          │
│              │  • BaseEntity        │                          │
│              │  • BusinessException │                          │
│              │  • DomainEvent       │                          │
│              │  • GlobalException   │                          │
│              │    Handler           │                          │
│              └───────────────────────┘                          │
└─────────────────────────────────────────────────────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                       INFRASTRUCTURE                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Product    │  │   Customer   │  │    Order     │         │
│  │   JPA Repo   │  │   JPA Repo   │  │   JPA Repo   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│         │                 │                 │                   │
└─────────┼─────────────────┼─────────────────┼───────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PostgreSQL Database                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  product_    │  │  customer_   │  │   order_     │         │
│  │  schema      │  │  schema      │  │   schema     │         │
│  │              │  │              │  │              │         │
│  │  products    │  │  customers   │  │  orders       │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 Fluxo de Dependências

```
┌─────────────────────────────────────────────────────────────┐
│                    SHARED (Núcleo Base)                      │
│  • BaseEntity                                                │
│  • BusinessException                                         │
│  • DomainEvent / BaseDomainEvent                             │
│  • GlobalExceptionHandler                                    │
│                                                              │
│  ⚠️ NÃO DEPENDE DE NINGUÉM                                   │
└─────────────────────────────────────────────────────────────┘
                    ▲                    ▲
                    │                    │
        ┌───────────┴──────────┐         │
        │                      │         │
┌───────▼────────┐   ┌────────▼──────┐ │
│   CUSTOMER     │   │    PRODUCT     │ │
│                │   │                │ │
│  • Domain      │   │  • Domain      │ │
│  • Application │   │  • Application │ │
│  • Infra       │   │  • Infra       │ │
│  • Presentation│   │  • Presentation│ │
└────────────────┘   └────────────────┘ │
        │                      │         │
        └──────────┬───────────┘         │
                   │                     │
         ┌─────────▼─────────┐          │
         │       ORDER       │          │
         │                   │          │
         │  • Domain         │          │
         │  • Application    │          │
         │  • Infra          │          │
         │  • Presentation   │          │
         │                   │          │
         │  Referencia:      │          │
         │  • customerId     │          │
         │  • productId      │          │
         └─────────┬─────────┘          │
                   │                    │
         ┌─────────▼─────────┐          │
         │      PAYMENT      │          │
         │                   │          │
         │  • Domain         │          │
         │  • Application    │          │
         │  • Infra          │          │
         │  • Presentation   │          │
         │                   │          │
         │  Referencia:      │          │
         │  • orderId        │          │
         └───────────────────┘          │
                                        │
                            ┌───────────┘
                            │
                    ┌───────▼────────┐
                    │   Ecommerce    │
                    │  Application   │
                    │                │
                    │  @SpringBoot   │
                    │  Application   │
                    │                │
                    │  • Config      │
                    │  • Swagger     │
                    │  • MockData    │
                    └────────────────┘
```

## 🚀 Ordem de Construção (Se Fosse Refazer)

### ✅ PASSO 1: SHARED (Fundação - OBRIGATÓRIO)

**Por quê começar aqui?**
- É a base de tudo
- Todos os módulos dependem dele
- Não depende de ninguém
- Define contratos genéricos

**O que colocar no Shared?**

```java
shared/
├── domain/
│   ├── BaseEntity.java              ✅ TODAS entidades herdam
│   └── event/
│       ├── DomainEvent.java         ✅ Interface para eventos
│       └── BaseDomainEvent.java     ✅ Base para eventos futuros
│
└── exception/
    ├── BusinessException.java       ✅ TODAS exceções herdam
    └── GlobalExceptionHandler.java  ✅ Trata todas exceções
```

**Por que ESSAS coisas no Shared?**

1. **BaseEntity**
   - Todas as entidades precisam: `id`, `createdAt`, `updatedAt`, `version`
   - Evita duplicação de código
   - Garante consistência

2. **BusinessException**
   - Todas as exceções de negócio herdam dela
   - `GlobalExceptionHandler` trata polimorficamente
   - Shared não precisa conhecer exceções específicas

3. **DomainEvent / BaseDomainEvent**
   - Preparação para eventos futuros
   - Comunicação assíncrona entre módulos
   - Facilita migração para microsserviços

4. **GlobalExceptionHandler**
   - Tratamento centralizado de erros
   - Não precisa importar exceções específicas
   - Retorna respostas HTTP consistentes

**O que NÃO colocar no Shared?**

❌ **Nada específico de um módulo:**
- ❌ `OrderException` - Cada módulo tem suas próprias
- ❌ `PaymentStatus` - Cada módulo tem seus enums
- ❌ DTOs específicos - Cada módulo tem seus DTOs
- ❌ Services específicos - Cada módulo tem seus services

### ✅ PASSO 2: CUSTOMER ou PRODUCT (Módulos Independentes)

**Por quê?**
- Não dependem de outros módulos de negócio
- Apenas dependem do Shared
- São mais simples
- Podem ser desenvolvidos em paralelo

**Ordem sugerida:**
1. **Customer** (mais simples - só CRUD básico)
2. **Product** (um pouco mais complexo - tem regras de estoque)

**Estrutura de cada módulo:**

```
customer/
├── domain/
│   ├── model/
│   │   ├── Customer.java          ← Entidade com regras
│   │   └── CustomerStatus.java   ← Enum
│   ├── repository/
│   │   └── CustomerRepository.java ← Interface
│   └── exception/
│       └── CustomerException.java  ← Herda BusinessException
│
├── application/
│   ├── usecase/
│   │   ├── CreateCustomerUseCase.java
│   │   ├── GetCustomerUseCase.java
│   │   ├── UpdateCustomerUseCase.java
│   │   └── DeactivateCustomerUseCase.java
│   └── dto/
│       └── CustomerDTO.java
│
├── infrastructure/
│   └── repository/
│       └── JpaCustomerRepository.java ← Implementa CustomerRepository
│
└── presentation/
    ├── CustomerController.java
    └── CustomerRequest.java
```

### ✅ PASSO 3: ORDER (Módulo Complexo)

**Por quê depois?**
- Depende de Customer e Product (referencia por UUID)
- É um Aggregate Root complexo
- Tem regras de negócio mais elaboradas
- Gerencia múltiplas entidades (Order + OrderItem)

**Estrutura:**

```
order/
├── domain/
│   ├── model/
│   │   ├── Order.java           ← Aggregate Root
│   │   ├── OrderItem.java       ← Entidade filha
│   │   └── OrderStatus.java     ← Enum
│   ├── repository/
│   │   └── OrderRepository.java
│   └── exception/
│       └── OrderException.java
│
├── application/
│   ├── usecase/
│   │   ├── CreateOrderUseCase.java
│   │   ├── GetOrderUseCase.java
│   │   ├── AddItemToOrderUseCase.java
│   │   ├── PayOrderUseCase.java
│   │   └── CancelOrderUseCase.java
│   └── dto/
│       ├── OrderDTO.java
│       └── OrderItemDTO.java
│
├── infrastructure/
│   └── repository/
│       └── JpaOrderRepository.java
│
└── presentation/
    ├── OrderController.java
    ├── OrderRequest.java
    └── AddItemRequest.java
```

### ✅ PASSO 4: PAYMENT (Depende de Order)

**Por quê por último?**
- Depende de Order (referencia `orderId`)
- Processa pagamentos de pedidos
- Pode ser desenvolvido depois que Order está pronto

## 📋 Checklist de Construção

### Fase 1: Fundação (Shared)
- [ ] Criar `BaseEntity` com campos comuns
- [ ] Criar `BusinessException` genérica
- [ ] Criar `GlobalExceptionHandler`
- [ ] Criar `DomainEvent` interface
- [ ] Criar `BaseDomainEvent` classe abstrata
- [ ] Configurar `application.yml` básico
- [ ] Configurar `pom.xml` com dependências

### Fase 2: Módulos Independentes
- [ ] **Customer:**
  - [ ] Domain (Customer, CustomerStatus, CustomerException, CustomerRepository)
  - [ ] Application (UseCases, DTOs)
  - [ ] Infrastructure (JpaCustomerRepository)
  - [ ] Presentation (Controller, Request)
  - [ ] Flyway migration (customer_schema)

- [ ] **Product:**
  - [ ] Domain (Product, ProductStatus, ProductException, ProductRepository)
  - [ ] Application (UseCases, DTOs)
  - [ ] Infrastructure (JpaProductRepository)
  - [ ] Presentation (Controller, Request)
  - [ ] Flyway migration (product_schema)

### Fase 3: Módulo Complexo
- [ ] **Order:**
  - [ ] Domain (Order, OrderItem, OrderStatus, OrderException, OrderRepository)
  - [ ] Application (UseCases, DTOs)
  - [ ] Infrastructure (JpaOrderRepository)
  - [ ] Presentation (Controller, Requests)
  - [ ] Flyway migration (order_schema)

### Fase 4: Módulo Dependente
- [ ] **Payment:**
  - [ ] Domain (Payment, PaymentStatus, PaymentException)
  - [ ] Application (UseCases, DTOs)
  - [ ] Infrastructure (JpaPaymentRepository)
  - [ ] Presentation (Controller, Request)
  - [ ] Flyway migration (payment_schema)

## 7️⃣ Evolução natural (sem exagero)

### Etapa 1 — simples (recomendado pra você agora)
- **Processamento direto** no `ProcessPaymentUseCase` (sem adapter)
- **Persistência** via `JpaPaymentRepository` (Spring Data JPA)
- **Regras** no próprio use case (entidade simples/anêmica)

### Etapa 2 — quando começar a crescer
- Mover regras para o domínio (ex: `Payment.approve()` / `Payment.fail()`)
- Extrair um adapter leve:
  - `PaymentProcessor` (interface no domínio)
  - `StripePaymentProcessor` / `PayPalPaymentProcessor` (implementações na infra)
- Motivo: múltiplos provedores, testes mais fáceis, isolamento da integração externa

### Fase 5: Configuração Final
- [ ] Criar `EcommerceApplication` (main class)
- [ ] Configurar Swagger
- [ ] Criar `MockDataInitializer` (dados de teste)
- [ ] Testar todos os endpoints

## 🎯 Resumo: Por Onde Começar?

### ✅ SIM, COMECE PELO SHARED!

**Razões:**
1. **É a fundação** - Todos dependem dele
2. **Não tem dependências** - Pode ser feito isoladamente
3. **Define contratos** - BaseEntity, BusinessException, etc.
4. **Evita retrabalho** - Se fizer depois, terá que refatorar tudo

**O que colocar no Shared:**
- ✅ BaseEntity (entidades base)
- ✅ BusinessException (exceções base)
- ✅ GlobalExceptionHandler (tratamento de erros)
- ✅ DomainEvent / BaseDomainEvent (eventos futuros)

**O que NÃO colocar no Shared:**
- ❌ Exceções específicas (OrderException, ProductException)
- ❌ Enums específicos (OrderStatus, PaymentStatus)
- ❌ DTOs específicos
- ❌ Services específicos

**Ordem recomendada:**
1. **Shared** (fundação)
2. **Customer** (simples, independente)
3. **Product** (simples, independente)
4. **Order** (complexo, depende de Customer/Product)
5. **Payment** (depende de Order)

## 🔍 Exemplo Prático: Criar Order

### Fluxo Completo:

```
1. HTTP Request
   POST /api/orders
   { "customerId": "uuid" }

2. OrderController
   ├─ Recebe OrderRequest
   ├─ Valida com @Valid
   └─ Chama createOrderUseCase.execute()

3. CreateOrderUseCase
   ├─ Cria Order order = new Order()
   ├─ order.setCustomerId(customerId)
   ├─ order = repository.save(order)  ← Vai para infra
   └─ Retorna OrderDTO.from(order)

4. Order (Domain)
   ├─ Herda de BaseEntity (shared)
   │  • id (UUID)
   │  • createdAt
   │  • updatedAt
   │  • version
   ├─ Regras de negócio:
   │  • addItem() - recalcula total
   │  • pay() - valida transição
   │  • cancel() - valida se pode
   └─ Lança OrderException se erro

5. JpaOrderRepository (Infrastructure)
   ├─ Implementa OrderRepository (interface do domínio)
   ├─ Estende JpaRepository (Spring Data)
   └─ Persiste no PostgreSQL (order_schema.orders)

6. Se erro:
   OrderException → BusinessException → GlobalExceptionHandler
   Retorna HTTP 400 com mensagem de erro
```

## 💡 Dicas Finais

1. **Sempre comece pelo Shared** - É a base de tudo
2. **Módulos independentes primeiro** - Customer e Product podem ser paralelos
3. **Módulos complexos depois** - Order precisa de Customer/Product
4. **Módulos dependentes por último** - Payment precisa de Order
5. **Teste cada camada** - Domain → Application → Infrastructure → Presentation
6. **Use Flyway desde o início** - Controle de versão do banco
7. **Mantenha Shared genérico** - Não coloque nada específico lá
