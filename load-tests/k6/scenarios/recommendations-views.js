import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, jsonHeaders, standardOptions } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';

export const options = standardOptions('recommendations-views', 2500);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const res = http.post(
    `${config.baseUrl}/api/recommendations/customers/${data.customerId}/views`,
    JSON.stringify({ productId: data.productId }),
    { ...jsonHeaders, tags: { module: 'recommendation', endpoint: 'recommendations-views', store: 'redis' } }
  );

  check(res, { 'views 204': (r) => r.status === 204 });
  sleep(config.pause);
}
