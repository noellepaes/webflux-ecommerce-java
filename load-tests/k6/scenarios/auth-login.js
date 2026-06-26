import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, jsonHeaders, standardOptions } from '../lib/config.js';

export const options = standardOptions('login', 2000);

export default function () {
  const res = http.post(
    `${config.baseUrl}/api/auth/login`,
    JSON.stringify({ email: config.email, password: config.password }),
    { ...jsonHeaders, tags: { module: 'auth', endpoint: 'login', store: 'postgres' } }
  );

  check(res, { 'login 200': (r) => r.status === 200 });
  sleep(config.pause);
}
