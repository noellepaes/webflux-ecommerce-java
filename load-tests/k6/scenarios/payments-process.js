import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, jsonHeaders, standardOptions } from '../lib/config.js';
import { bootstrap, createOrder, addItemToOrder } from '../lib/setup.js';

export const options = standardOptions('payments-process', 4000);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const order = createOrder(data.customerId);
  addItemToOrder(order.id, data);

  const res = http.post(
    `${config.baseUrl}/api/payments`,
    JSON.stringify({
      orderId: order.id,
      amount: data.productPrice,
      method: 'PIX',
    }),
    { ...jsonHeaders, tags: { module: 'payment', endpoint: 'payments-process', store: 'postgres' } }
  );

  check(res, { 'payment 201': (r) => r.status === 201 });
  sleep(config.pause);
}
