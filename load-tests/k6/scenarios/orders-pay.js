import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, standardOptions } from '../lib/config.js';
import { bootstrap, createOrder, addItemToOrder } from '../lib/setup.js';

export const options = standardOptions('orders-pay', 3500);

export function setup() {
  return bootstrap();
}

export default function (data) {
  const order = createOrder(data.customerId);
  addItemToOrder(order.id, data);

  const res = http.post(`${config.baseUrl}/api/orders/${order.id}/pay`, null, {
    tags: { module: 'order', endpoint: 'orders-pay', store: 'postgres' },
  });

  check(res, { 'order pay 200': (r) => r.status === 200 });
  sleep(config.pause);
}
