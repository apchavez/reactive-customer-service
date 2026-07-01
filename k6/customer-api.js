import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Configuration ────────────────────────────────────────────────────────────

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN    = __ENV.API_TOKEN || '';

const STAGES = {
  smoke: [
    { duration: '30s', target: 1 },
  ],
  load: [
    { duration: '30s', target:  5 },
    { duration: '1m',  target: 20 },
    { duration: '30s', target:  0 },
  ],
  spike: [
    { duration: '10s', target: 50 },
    { duration: '30s', target: 50 },
    { duration: '10s', target:  0 },
  ],
};

export const options = {
  stages: STAGES[__ENV.SCENARIO || 'smoke'],
  thresholds: {
    http_req_failed:                         ['rate<0.01'],
    http_req_duration:                       ['p(95)<500', 'p(99)<1000'],
    'group_duration{group:::crud}':          ['p(95)<2000'],
    'group_duration{group:::list-active}':   ['p(95)<300'],
  },
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

const HEADERS = {
  'Content-Type': 'application/json',
  Authorization: `Bearer ${TOKEN}`,
};

function randomSuffix() {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

function buildCustomer(suffix) {
  return JSON.stringify({
    nombre:   `Load${suffix}`,
    apellido: 'K6',
    estado:   'ACTIVE',
    edad:     30,
  });
}

// ─── Main scenario ────────────────────────────────────────────────────────────

export default function () {
  const suffix = randomSuffix();

  group('crud', () => {
    // POST — create
    const createRes = http.post(
      `${BASE_URL}/api/v1/customers`,
      buildCustomer(suffix),
      { headers: HEADERS }
    );
    const created = check(createRes, {
      'POST /customers → 201': r => r.status === 201,
      'POST response has id':  r => r.json('id') !== undefined,
    });
    if (!created) return;

    const id = createRes.json('id');

    // GET by ID
    const getRes = http.get(`${BASE_URL}/api/v1/customers/${id}`, { headers: HEADERS });
    check(getRes, {
      'GET /customers/{id} → 200':    r => r.status === 200,
      'GET response nombre matches':  r => r.json('nombre') === `Load${suffix}`,
    });

    // PUT — update
    const putRes = http.put(
      `${BASE_URL}/api/v1/customers/${id}`,
      JSON.stringify({ nombre: `Upd${suffix}`, apellido: 'K6', estado: 'INACTIVE', edad: 31 }),
      { headers: HEADERS }
    );
    check(putRes, { 'PUT /customers/{id} → 200': r => r.status === 200 });

    // DELETE
    const delRes = http.del(
      `${BASE_URL}/api/v1/customers/${id}`,
      null,
      { headers: HEADERS }
    );
    check(delRes, { 'DELETE /customers/{id} → 204': r => r.status === 204 });
  });

  group('list-active', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/customers/active?page=0&size=20`,
      { headers: HEADERS }
    );
    check(res, { 'GET /customers/active → 200': r => r.status === 200 });
  });

  sleep(0.5);
}
