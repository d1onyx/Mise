import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    recipe_feed_and_pantry_filters: {
      executor: 'constant-vus',
      vus: 10,
      duration: '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.AUTH_TOKEN || ':load-test-user';

export default function () {
  const headers = {
    Authorization: `Bearer ${token}`,
    'X-Request-Id': `k6-${__VU}-${__ITER}`,
  };

  const recipeFeed = http.get(`${baseUrl}/api/v1/recipes?q=soup&tag=dinner&page=1&pageSize=20&sort=recent`, { headers });
  check(recipeFeed, {
    'recipe feed responds': (response) => response.status === 200,
  });

  sleep(1);
}
