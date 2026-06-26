import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';

export const options = standardOptions('products-by-id', 1500);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const res = http.get(`${config.baseUrl}/api/products/${data.productId}`, {
    tags: { module: 'product', endpoint: 'products-by-id', store: 'postgres' },
  });

  check(res, { 'product by id 200': (r) => r.status === 200 });
  sleep(config.pause);
}
