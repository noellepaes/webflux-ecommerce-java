# 🚀 Guia Rápido

## 1. Subir o Banco de Dados

```bash
docker-compose up -d
```

Verificar se está rodando:
```bash
docker ps
```

## 2. Executar a Aplicação

```bash
mvn spring-boot:run
```

Ou com Maven wrapper:
```bash
./mvnw spring-boot:run
```

## 3. Testar os Endpoints

### Criar um Cliente

```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com",
    "cpf": "12345678900"
  }'
```

**Resposta**: Anote o `id` do cliente criado.

### Criar um Produto

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Notebook",
    "description": "Notebook Dell",
    "price": 3500.00,
    "stock": 10
  }'
```

**Resposta**: Anote o `id` do produto criado.

### Criar um Pedido

```bash
curl -X POST "http://localhost:8080/api/orders?customerId=SEU_CUSTOMER_ID"
```

**Resposta**: Anote o `id` do pedido criado.

### Adicionar Item ao Pedido

```bash
curl -X POST "http://localhost:8080/api/orders/SEU_ORDER_ID/items?productId=SEU_PRODUCT_ID&productName=Notebook&quantity=2&unitPrice=3500.00"
```

### Pagar o Pedido

```bash
curl -X POST http://localhost:8080/api/orders/SEU_ORDER_ID/pay
```

### Criar Pagamento

```bash
curl -X POST "http://localhost:8080/api/payments?orderId=SEU_ORDER_ID&amount=7000.00&method=CREDIT_CARD"
```

### Aprovar Pagamento

```bash
curl -X POST http://localhost:8080/api/payments/SEU_PAYMENT_ID/approve
```

## 4. Verificar Schemas no Banco

Conectar ao PostgreSQL:
```bash
docker exec -it ecommerce-postgres psql -U ecommerce -d ecommerce
```

Listar schemas:
```sql
\dn
```

Verificar tabelas em cada schema:
```sql
\dt order_schema.*
\dt product_schema.*
\dt customer_schema.*
\dt payment_schema.*
```

## 5. Testar Regras de Negócio

### Teste 1: Pedido só pode ir de PENDING → PAID

```bash
# Criar pedido
ORDER_ID=$(curl -s -X POST "http://localhost:8080/api/orders?customerId=SEU_CUSTOMER_ID" | jq -r '.id')

# Tentar pagar (deve funcionar)
curl -X POST "http://localhost:8080/api/orders/$ORDER_ID/pay"

# Tentar pagar novamente (deve falhar)
curl -X POST "http://localhost:8080/api/orders/$ORDER_ID/pay"
```

### Teste 2: Não pode reduzir estoque abaixo de zero

```bash
# Criar produto com estoque 5
PRODUCT_ID=$(curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Produto Teste", "price": 100.00, "stock": 5}' | jq -r '.id')

# Tentar reduzir 10 unidades (deve falhar)
curl -X POST "http://localhost:8080/api/products/$PRODUCT_ID/decrease-stock?quantity=10"
```

## 6. Testes de carga (k6)

```powershell
cd load-tests
.\run-suite.ps1 -Vus 50 -Duration "30s"
.\run-stress.ps1 -Vus 200 -Duration "45s" -GraphPeers 100
```

Resumos em `load-tests/results/` (sem dumps `.log` no repo). Detalhes e tabelas: `README.md`.

### Por que economizar threads sai mais barato?

Não porque cada request gaste menos CPU — e sim porque **mais clientes cabem na mesma máquina**:

- Cada thread de plataforma custa memória (~1 MB de stack + objetos). Tomcat com centenas de threads inchadas → VM maior ou OOM mais cedo.
- WebFlux manteve ~**40 threads com 500 VUs** em `GET /api/products`; modelo bloqueante tenderia a crescer o pool com a concorrência.
- Na prática: **menos réplicas / instâncias menores** para a mesma carga I/O-bound → conta de cloud menor. bcrypt/SQL pesado continuam caros.

## 7. Verificar Logs

Os logs do Spring Boot mostrarão:
- Schemas sendo criados pelo Flyway
- SQL sendo executado
- Erros de validação

## 8. Parar a Aplicação

```bash
# Parar aplicação: Ctrl+C

# Parar banco de dados
docker-compose down

# Parar e remover volumes (limpar dados)
docker-compose down -v
```
