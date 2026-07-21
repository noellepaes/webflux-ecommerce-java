import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';
import { densifyRecommendationGraph } from '../lib/graph-seed.js';

/**
 * Stress GET /api/recommendations/customers/{id} after densifying the Redis graph.
 * Default suite uses only DevSeed (tiny graph) — this targets fan-out + high concurrency.
 */
export const options = standardOptions('recommendations-read-stress', 15000);

export function setup() {
  const data = bootstrap();
  const productsRes = http.get(`${config.baseUrl}/api/products`);
  if (productsRes.status !== 200) {
    throw new Error(`Produtos indisponíveis: ${productsRes.status}`);
  }
  const products = productsRes.json();
  const productIds = products.map((p) => p.id);

  const graph = densifyRecommendationGraph(data.customerId, productIds);
  console.log(
    `graph-seed: peers=${graph.peers} overlap=${graph.overlapProducts} ` +
      `posts=${graph.postsOk}/${graph.postsTotal} fail=${graph.postsFail}`
  );

  return { ...data, graph };
}

export default function (data) {
  const res = http.get(
    `${config.baseUrl}/api/recommendations/customers/${data.customerId}`,
    {
      tags: {
        module: 'recommendation',
        endpoint: 'recommendations-read-stress',
        store: 'redis',
      },
    }
  );

  check(res, { 'recommendations stress 200': (r) => r.status === 200 });
  sleep(config.pause);
}
