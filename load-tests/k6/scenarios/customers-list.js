import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';

export const options = standardOptions('customers-list', 2000);

export default function () {
  const res = http.get(`${config.baseUrl}/api/customers`, {
    tags: { module: 'customer', endpoint: 'customers-list', store: 'postgres' },
  });

  check(res, { 'customers 200': (r) => r.status === 200 });
  sleep(config.pause);
}
