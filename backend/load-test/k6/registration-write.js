import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

const BASE_URL = 'http://localhost:8080';
const TEST_PASSWORD = 'Password123!';
const COURSE_ID = 16;

// user1@test.com ~ user5000@test.com 사용
const USERS = Array.from({ length: 5000 }, (_, i) => `user${i + 1}@test.com`);

export const options = {
    scenarios: {
        registration_once_per_user: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: 5000,
            maxDuration: '10m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500'],
    },
};

function login(email) {
    const payload = JSON.stringify({
        email: email,
        password: TEST_PASSWORD,
    });

    const res = http.post(`${BASE_URL}/api/v1/auth/login`, payload, {
        headers: {
            'Content-Type': 'application/json',
        },
    });

    const ok = check(res, {
        'login status is 200': (r) => r.status === 200,
        'accessToken cookie exists': (r) =>
            !!r.cookies.accessToken && r.cookies.accessToken.length > 0,
        'refreshToken cookie exists': (r) =>
            !!r.cookies.refreshToken && r.cookies.refreshToken.length > 0,
    });

    if (!ok) {
        throw new Error(`로그인 실패: email=${email}, status=${res.status}, body=${res.body}`);
    }

    return {
        accessToken: res.cookies.accessToken[0].value,
        refreshToken: res.cookies.refreshToken[0].value,
    };
}

export default function () {
    const iteration = exec.scenario.iterationInTest;
    const email = USERS[iteration];

    if (!email) {
        throw new Error(`유저 매핑 실패: iteration=${iteration}`);
    }

    const tokens = login(email);

    const payload = JSON.stringify({
        courseId: COURSE_ID,
        snapZipCode: '12345',
        snapAddress: '서울시 강남구',
        snapDetail: '101동',
        tSize: 'L',
        agreedTerms: true,
    });

    const res = http.post(`${BASE_URL}/api/v1/registrations`, payload, {
        cookies: {
            accessToken: tokens.accessToken,
            refreshToken: tokens.refreshToken,
        },
        headers: {
            'Content-Type': 'application/json',
        },
    });

    check(res, {
        'registration status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    });

    if (!(res.status === 200 || res.status === 201)) {
        console.log(`registration failed: email=${email}, status=${res.status}, body=${res.body}`);
    }
}