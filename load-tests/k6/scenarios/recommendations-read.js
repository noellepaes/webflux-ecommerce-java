import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';
import { bootstrap } from '../lib/setup.js';

export const options = standardOptions('recommendations-read', 3000);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const res = http.get(
    `${config.baseUrl}/api/recommendations/customers/${data.customerId}`,
    { tags: { module: 'recommendation', endpoint: 'recommendations-read', store: 'redis' } }
  );

  check(res, { 'recommendations 200': (r) => r.status === 200 });
  sleep(config.pause);
}
