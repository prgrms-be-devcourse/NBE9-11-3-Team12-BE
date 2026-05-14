import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

const BASE_URL = 'http://localhost:8080';
const TEST_PASSWORD = 'Password123!';
const COURSE_ID = 16;
const USERS = Array.from({ length: 40000 }, (_, i) => `user${i + 1}@test.com`);

export const options = {
    scenarios: {
        registration_once_per_user: {
            executor: 'shared-iterations',
            vus: 500,
            iterations: 40000,
            maxDuration: '10m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500'],
    },
};

function login(email) {
    const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
        email,
        password: TEST_PASSWORD,
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    const ok = check(res, {
        'login status is 200': (r) => r.status === 200,
        'accessToken cookie exists': (r) => !!r.cookies.accessToken?.length,
        'refreshToken cookie exists': (r) => !!r.cookies.refreshToken?.length,
    });

    if (!ok) throw new Error(`로그인 실패: ${email}`);
    return {
        accessToken: res.cookies.accessToken[0].value,
        refreshToken: res.cookies.refreshToken[0].value,
    };
}

export default function () {
    const email = USERS[exec.scenario.iterationInTest];
    const tokens = login(email);

    const res = http.post(`${BASE_URL}/api/v1/registrations`, JSON.stringify({
        courseId: COURSE_ID,
        snapZipCode: '12345',
        snapAddress: '서울시 강남구',
        snapDetail: '101동',
        tSize: 'L',
        agreedTerms: true,
    }), {
        cookies: tokens,
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, {
        'registration status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    });
}