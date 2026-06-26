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
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({
      email: 'noelle.seed@dev.local',
      password: '123456',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (loginRes.status !== 200) {
    throw new Error(`Login falhou: ${loginRes.status} ${loginRes.body}`);
  }

  const session = loginRes.json();
  const productsRes = http.get(`${BASE_URL}/api/products`);

  if (productsRes.status !== 200) {
    throw new Error(`Listagem de produtos falhou: ${productsRes.status}`);
  }

  const products = productsRes.json();
  if (!products.length) {
    throw new Error('Nenhum produto encontrado para o teste misto');
  }

  return {
    customerId: session.customerId,
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
