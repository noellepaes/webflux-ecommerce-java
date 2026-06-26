import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';

export const options = standardOptions('users', 1500);

export default function () {
  const res = http.get(`${config.baseUrl}/api/auth/users`, {
    tags: { module: 'auth', endpoint: 'users', store: 'postgres' },
  });

  check(res, { 'users 200': (r) => r.status === 200 });
  sleep(config.pause);
}
