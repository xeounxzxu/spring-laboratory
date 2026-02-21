import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';
const PATH = __ENV.PATH || '/coroutines/pub-ping';
const SLEEP_MS = Number(__ENV.SLEEP_MS || '0');

export const options = {
  scenarios: {
    pub_ping_single_node_200_threads: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 150 },
        { duration: '30s', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const requestId = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const url = `${BASE_URL}${PATH}?requestId=${encodeURIComponent(requestId)}`;

  const res = http.get(url, {
    headers: {
      Accept: 'application/json',
    },
    tags: {
      endpoint: 'coroutines_pub_ping',
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'content-type is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
    'response has same requestId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.requestId === requestId;
      } catch (e) {
        return false;
      }
    },
  });

  if (SLEEP_MS > 0) {
    sleep(SLEEP_MS / 1000);
  }
}
