import http from 'k6/http';
import { config, jsonHeaders } from './config.js';

/**
 * Bootstrap sem login: usa o customer seed (email) + primeiro produto do catálogo.
 */
export function bootstrap() {
  const customersRes = http.get(`${config.baseUrl}/api/customers`);
  if (customersRes.status !== 200) {
    throw new Error(`Clientes indisponíveis: ${customersRes.status}`);
  }

  const customers = customersRes.json();
  if (!customers.length) {
    throw new Error('Nenhum cliente no seed');
  }

  const customer =
    customers.find((c) => c.email === config.email) || customers[0];

  const productsRes = http.get(`${config.baseUrl}/api/products`);
  if (productsRes.status !== 200) {
    throw new Error(`Produtos indisponíveis: ${productsRes.status}`);
  }

  const products = productsRes.json();
  if (!products.length) {
    throw new Error('Nenhum produto no catálogo');
  }

  const product = products[0];

  return {
    customerId: customer.id,
    productId: product.id,
    productName: product.name,
    productPrice: product.price,
  };
}

export function createOrder(customerId) {
  const res = http.post(
    `${config.baseUrl}/api/orders`,
    JSON.stringify({ customerId }),
    jsonHeaders
  );

  if (res.status !== 201) {
    throw new Error(`Criar pedido falhou: ${res.status} ${res.body}`);
  }

  return res.json();
}

export function addItemToOrder(orderId, product) {
  const res = http.post(
    `${config.baseUrl}/api/orders/${orderId}/items`,
    JSON.stringify({
      productId: product.productId,
      productName: product.productName,
      quantity: 1,
      unitPrice: product.productPrice,
    }),
    jsonHeaders
  );

  if (res.status !== 200) {
    throw new Error(`Adicionar item falhou: ${res.status} ${res.body}`);
  }

  return res.json();
}
