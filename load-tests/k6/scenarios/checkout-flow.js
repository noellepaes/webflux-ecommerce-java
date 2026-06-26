import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, jsonHeaders } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';

export const options = {
  vus: Number(__ENV.VUS || 30),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.15'],
    'http_req_duration{endpoint:checkout}': ['p(95)<8000'],
  },
};

export function setup() {
  return bootstrap();
}

export default function (data) {
  const orderRes = http.post(
    `${config.baseUrl}/api/orders`,
    JSON.stringify({ customerId: data.customerId }),
    { ...jsonHeaders, tags: { module: 'checkout', endpoint: 'checkout', step: 'create-order' } }
  );
  check(orderRes, { 'checkout order 201': (r) => r.status === 201 });

  if (orderRes.status !== 201) {
    sleep(config.pause);
    return;
  }

  const orderId = orderRes.json().id;

  const itemRes = http.post(
    `${config.baseUrl}/api/orders/${orderId}/items`,
    JSON.stringify({
      productId: data.productId,
      productName: data.productName,
      quantity: 1,
      unitPrice: data.productPrice,
    }),
    { ...jsonHeaders, tags: { module: 'checkout', endpoint: 'checkout', step: 'add-item' } }
  );
  check(itemRes, { 'checkout item 200': (r) => r.status === 200 });

  const paymentRes = http.post(
    `${config.baseUrl}/api/payments`,
    JSON.stringify({
      orderId,
      amount: data.productPrice,
      method: 'PIX',
    }),
    { ...jsonHeaders, tags: { module: 'checkout', endpoint: 'checkout', step: 'payment' } }
  );
  check(paymentRes, { 'checkout payment 201': (r) => r.status === 201 });

  sleep(config.pause);
}
