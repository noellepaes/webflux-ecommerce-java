import http from 'k6/http';
import { config, jsonHeaders } from './config.js';

export function bootstrap() {
  const loginRes = http.post(
    `${config.baseUrl}/api/auth/login`,
    JSON.stringify({ email: config.email, password: config.password }),
    jsonHeaders
  );

  if (loginRes.status !== 200) {
    throw new Error(`Login falhou: ${loginRes.status} ${loginRes.body}`);
  }

  const session = loginRes.json();
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
    customerId: session.customerId,
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
