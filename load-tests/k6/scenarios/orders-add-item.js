import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, jsonHeaders, standardOptions } from '../lib/config.js';
import { bootstrap, createOrder } from '../lib/setup.js';

export const options = standardOptions('orders-add-item', 3500);

export function setup() {
  const data = bootstrap();
  const order = createOrder(data.customerId);
  return { ...data, orderId: order.id };
}

export default function (data) {
  const res = http.post(
    `${config.baseUrl}/api/orders/${data.orderId}/items`,
    JSON.stringify({
      productId: data.productId,
      productName: data.productName,
      quantity: 1,
      unitPrice: data.productPrice,
    }),
    { ...jsonHeaders, tags: { module: 'order', endpoint: 'orders-add-item', store: 'postgres' } }
  );

  check(res, { 'add item 200': (r) => r.status === 200 });
  sleep(config.pause);
}
