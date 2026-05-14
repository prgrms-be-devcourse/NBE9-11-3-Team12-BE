import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

const BASE_URL = 'http://localhost:8080';
const TEST_PASSWORD = 'Password123!';

// user1 ~ user10000
const USERS = Array.from({ length: 10000 }, (_, i) => `user${i + 1}@test.com`);

export const options = {
    stages: [
        { duration: '30s', target: 30 },
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 150 },
        { duration: '30s', target: 200 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {
    const idx = exec.scenario.iterationInTest % USERS.length;
    const email = USERS[idx];

    const payload = JSON.stringify({
        email: email,
        password: TEST_PASSWORD,
    });

    const res = http.post(`${BASE_URL}/api/v1/auth/login`, payload, {
        headers: {
            'Content-Type': 'application/json',
        },
    });

    check(res, {
        'login status is 200': (r) => r.status === 200,
        'accessToken cookie exists': (r) =>
            !!r.cookies.accessToken && r.cookies.accessToken.length > 0,
        'refreshToken cookie exists': (r) =>
            !!r.cookies.refreshToken && r.cookies.refreshToken.length > 0,
    });

    sleep(1);
}