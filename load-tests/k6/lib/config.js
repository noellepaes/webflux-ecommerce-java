export const config = {
  baseUrl: __ENV.BASE_URL || 'http://localhost:8080',
  vus: Number(__ENV.VUS || 50),
  duration: __ENV.DURATION || '30s',
  pause: Number(__ENV.PAUSE || 0.1),
  email: __ENV.TEST_EMAIL || 'noelle.seed@dev.local',
  password: __ENV.TEST_PASSWORD || '123456',
};

export const jsonHeaders = {
  headers: { 'Content-Type': 'application/json' },
};

export function standardOptions(endpoint, p95Ms = 3000) {
  return {
    vus: config.vus,
    duration: config.duration,
    thresholds: {
      http_req_failed: ['rate<0.10'],
      [`http_req_duration{endpoint:${endpoint}}`]: [`p(95)<${p95Ms}`],
    },
  };
}
