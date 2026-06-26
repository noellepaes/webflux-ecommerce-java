import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, jsonHeaders, standardOptions } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';

export const options = standardOptions('orders-create', 3000);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const res = http.post(
    `${config.baseUrl}/api/orders`,
    JSON.stringify({ customerId: data.customerId }),
    { ...jsonHeaders, tags: { module: 'order', endpoint: 'orders-create', store: 'postgres' } }
  );

  check(res, { 'order create 201': (r) => r.status === 201 });
  sleep(config.pause);
}
