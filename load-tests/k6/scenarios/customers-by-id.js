import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';

export const options = standardOptions('customers-by-id', 2000);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const res = http.get(`${config.baseUrl}/api/customers/${data.customerId}`, {
    tags: { module: 'customer', endpoint: 'customers-by-id', store: 'postgres' },
  });

  check(res, { 'customer by id 200': (r) => r.status === 200 });
  sleep(config.pause);
}
