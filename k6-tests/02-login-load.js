import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, USERNAME, PASSWORD, jsonHeaders } from './common.js';

export const options = {
    stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 30 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],
        'http_req_duration{name:login}': ['p(95)<800', 'p(99)<1500'],
    },
};

export default function () {
    const res = http.post(
        `${BASE_URL}/auth/public/signin`,
        JSON.stringify({
            username: USERNAME,
            password: PASSWORD,
        }),
        {
            ...jsonHeaders(),
            tags: { name: 'login' },
        }
    );

    check(res, {
        'login status is 200': (r) => r.status === 200,
        'login has jwtToken': (r) => {
            try {
                return !!r.json('jwtToken');
            } catch (e) {
                return false;
            }
        },
    });

    sleep(1);
}