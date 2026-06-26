import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: Number(__ENV.VUS || 100),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const response = http.get(`${BASE_URL}/api/products`);

  check(response, {
    'status 200': (r) => r.status === 200,
    'retornou JSON': (r) => r.headers['Content-Type']?.includes('application/json'),
  });

  sleep(0.1);
}
