import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';

/**
 * High-concurrency stress for GET /api/products.
 * The suite already runs products-list.js at ~50 VUs; this pushes connection pressure
 * to show WebFlux thread stability (not lower p95 vs JPA on tiny catalogs).
 */
export const options = standardOptions('products-list-stress', 10000);

export default function () {
  const res = http.get(`${config.baseUrl}/api/products`, {
    tags: { module: 'product', endpoint: 'products-list-stress', store: 'postgres' },
  });

  check(res, { 'products stress 200': (r) => r.status === 200 });
  sleep(config.pause);
}
