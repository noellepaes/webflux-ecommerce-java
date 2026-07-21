import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '10s', target: 20 },
    { duration: '30s', target: 100 },
    { duration: '20s', target: 100 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    'http_req_duration{endpoint:products}': ['p(95)<1500'],
    'http_req_duration{endpoint:recommendations}': ['p(95)<2000'],
    'http_req_duration{endpoint:views}': ['p(95)<2000'],
  },
};

export function setup() {
  const customersRes = http.get(`${BASE_URL}/api/customers`);
  if (customersRes.status !== 200) {
    throw new Error(`Clientes indisponíveis: ${customersRes.status}`);
  }

  const customers = customersRes.json();
  const customer =
    customers.find((c) => c.email === 'noelle.seed@dev.local') || customers[0];
  if (!customer) {
    throw new Error('Nenhum cliente no seed');
  }

  const productsRes = http.get(`${BASE_URL}/api/products`);
  if (productsRes.status !== 200) {
    throw new Error(`Listagem de produtos falhou: ${productsRes.status}`);
  }

  const products = productsRes.json();
  if (!products.length) {
    throw new Error('Nenhum produto encontrado para o teste misto');
  }

  return {
    customerId: customer.id,
    productId: products[0].id,
  };
}

export default function (data) {
  const productsRes = http.get(`${BASE_URL}/api/products`, {
    tags: { endpoint: 'products' },
  });
  check(productsRes, { 'products 200': (r) => r.status === 200 });

  const suggestionsRes = http.get(
    `${BASE_URL}/api/recommendations/customers/${data.customerId}`,
    { tags: { endpoint: 'recommendations' } }
  );
  check(suggestionsRes, { 'recommendations 200': (r) => r.status === 200 });

  const viewRes = http.post(
    `${BASE_URL}/api/recommendations/customers/${data.customerId}/views`,
    JSON.stringify({ productId: data.productId }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'views' },
    }
  );
  check(viewRes, { 'views 204': (r) => r.status === 204 });

  sleep(0.2);
}
