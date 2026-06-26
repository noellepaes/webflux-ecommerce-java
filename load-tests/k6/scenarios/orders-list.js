import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';

export const options = standardOptions('orders-list', 2500);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const res = http.get(`${config.baseUrl}/api/orders/customer/${data.customerId}`, {
    tags: { module: 'order', endpoint: 'orders-list', store: 'postgres' },
  });

  check(res, { 'orders list 200': (r) => r.status === 200 });
  sleep(config.pause);
}
