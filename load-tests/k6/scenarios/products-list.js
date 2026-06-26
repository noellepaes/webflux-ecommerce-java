import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';

export const options = standardOptions('products-list', 1500);

export default function () {
  const res = http.get(`${config.baseUrl}/api/products`, {
    tags: { module: 'product', endpoint: 'products-list', store: 'postgres' },
  });

  check(res, { 'products 200': (r) => r.status === 200 });
  sleep(config.pause);
}
