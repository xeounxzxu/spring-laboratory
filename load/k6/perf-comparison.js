import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const ENDPOINT = __ENV.ENDPOINT || 'blocking';
const SLEEP_MS = Number(__ENV.SLEEP_MS || '50');
const PATH_MAP = {
  blocking: '/perf/blocking',
  suspend: '/perf/suspend',
  'io-blocking': '/perf-io/blocking',
  'io-suspend': '/perf-io/suspend',
};
const PATH = PATH_MAP[ENDPOINT] || '/perf/blocking';

const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '50');
const MAX_VUS = Number(__ENV.MAX_VUS || '400');
const RATE = Number(__ENV.RATE || '200');
const DURATION = __ENV.DURATION || '30s';

export const options = {
  scenarios: {
    perf_probe: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const url = `${BASE_URL}${PATH}?sleepMs=${SLEEP_MS}`;

  const res = http.get(url, {
    headers: {
      Accept: 'application/json',
    },
    tags: {
      endpoint: ENDPOINT,
      sleep_ms: String(SLEEP_MS),
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'content-type is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
    'response endpoint matches': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.endpoint === ENDPOINT;
      } catch (e) {
        return false;
      }
    },
  });
}
