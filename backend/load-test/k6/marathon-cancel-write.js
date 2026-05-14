import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';

const BASE_URL = 'http://localhost:8080';
const ORGANIZER_EMAIL = 'organizer@test.com';
const ORGANIZER_PASSWORD = 'Password123!';

const MARATHON_START_ID = 17;
const COUNT = 5000;

export const options = {
    scenarios: {
        cancel_once_per_marathon: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: COUNT,
            maxDuration: '10m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500'],
    },
};

function login() {
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: ORGANIZER_EMAIL,
            password: ORGANIZER_PASSWORD,
        }),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    const ok = check(res, {
        'login status is 200': (r) => r.status === 200,
        'accessToken cookie exists': (r) =>
            !!r.cookies.accessToken && r.cookies.accessToken.length > 0,
        'refreshToken cookie exists': (r) =>
            !!r.cookies.refreshToken && r.cookies.refreshToken.length > 0,
    });

    if (!ok) {
        console.log(`login failed: status=${res.status}, body=${res.body}`);
        fail('주최자 로그인 실패');
    }

    return {
        accessToken: res.cookies.accessToken[0].value,
        refreshToken: res.cookies.refreshToken[0].value,
    };
}

export function setup() {
    return login();
}

export default function (tokens) {
    const iteration = exec.scenario.iterationInTest;
    const marathonId = MARATHON_START_ID + iteration;

    const res = http.patch(
        `${BASE_URL}/api/v1/marathons/${marathonId}/cancel`,
        null,
        {
            cookies: {
                accessToken: tokens.accessToken,
                refreshToken: tokens.refreshToken,
            },
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    check(res, {
        'cancel status is 200': (r) => r.status === 200,
    });

    if (res.status !== 200) {
        console.log(`cancel failed: marathonId=${marathonId}, status=${res.status}, body=${res.body}`);
    }
}