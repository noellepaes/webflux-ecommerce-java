import http from 'k6/http';
import { config, jsonHeaders } from './config.js';

/**
 * Builds a denser Redis co-view graph than DevSeed (3 customers / few edges).
 * Synthetic peer customerIds are valid UUIDs — POST /views does not require DB rows.
 */
export function densifyRecommendationGraph(customerId, productIds, opts = {}) {
  const overlapProducts = Math.min(
    Number(opts.overlapProducts || __ENV.GRAPH_PRODUCTS || 5),
    productIds.length
  );
  const peers = Number(opts.peers || __ENV.GRAPH_PEERS || 80);
  const extraViewsPerPeer = Number(opts.extraViewsPerPeer || __ENV.GRAPH_EXTRA || 3);

  const shared = productIds.slice(0, overlapProducts);
  const extras = productIds.slice(overlapProducts);
  const requests = [];

  for (const productId of shared) {
    requests.push(viewReq(customerId, productId));
  }

  for (let i = 0; i < peers; i++) {
    const peerCustomerId = syntheticPeerId(i);
    for (const productId of shared) {
      requests.push(viewReq(peerCustomerId, productId));
    }
    for (let j = 0; j < Math.min(extraViewsPerPeer, extras.length); j++) {
      const productId = extras[(i + j) % extras.length];
      requests.push(viewReq(peerCustomerId, productId));
    }
  }

  const batchSize = 100;
  let ok = 0;
  let fail = 0;
  for (let offset = 0; offset < requests.length; offset += batchSize) {
    const chunk = requests.slice(offset, offset + batchSize);
    const responses = http.batch(chunk);
    for (const res of responses) {
      if (res.status === 204) ok += 1;
      else fail += 1;
    }
  }

  return {
    customerId,
    overlapProducts,
    peers,
    extraViewsPerPeer,
    postsOk: ok,
    postsFail: fail,
    postsTotal: requests.length,
  };
}

function viewReq(customerId, productId) {
  return {
    method: 'POST',
    url: `${config.baseUrl}/api/recommendations/customers/${customerId}/views`,
    body: JSON.stringify({ productId }),
    params: {
      ...jsonHeaders,
      tags: { module: 'recommendation', endpoint: 'graph-seed', store: 'redis' },
    },
  };
}

function syntheticPeerId(index) {
  const hex = index.toString(16).padStart(12, '0');
  return `b0000000-0000-4000-8000-${hex}`;
}
