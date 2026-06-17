import http from 'k6/http';
import { check, group, sleep } from 'k6';
import {
    BASE_URL,
    USERNAME,
    PASSWORD,
    USER_ACCOUNT_1,
    login,
    jsonHeaders,
} from './common.js';

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '2m', target: 50 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        checks: ['rate>0.99'],
        http_req_failed: ['rate<0.01'],

        'http_req_duration{name:my_accounts}': ['p(95)<500', 'p(99)<1000'],
        'http_req_duration{name:transaction_history_first_page}': ['p(95)<700', 'p(99)<1500'],
        'http_req_duration{name:transaction_history_deep_page}': ['p(95)<1200', 'p(99)<2500'],
    },
};

export function setup() {
    const token = login(USERNAME, PASSWORD);
    return { token };
}

export default function (data) {
    const token = data.token;

    group('my accounts', () => {
        const res = http.get(`${BASE_URL}/accounts/my-accounts`, {
            ...jsonHeaders(token),
            tags: { name: 'my_accounts' },
        });

        if (res.status !== 200) {
            console.error(`my accounts failed. status=${res.status}, body=${res.body}`);
        }

        check(res, {
            'my accounts status is 200': (r) => r.status === 200,
            'my accounts is array': (r) => {
                try {
                    return Array.isArray(r.json());
                } catch (e) {
                    return false;
                }
            },
        });
    });

    group('transaction history first page', () => {
        const res = http.get(
            `${BASE_URL}/accounts/${USER_ACCOUNT_1}/transactions?pageNo=0&pageSize=20`,
            {
                ...jsonHeaders(token),
                tags: { name: 'transaction_history_first_page' },
            }
        );

        if (res.status !== 200) {
            console.error(`transaction first page failed. status=${res.status}, body=${res.body}`);
        }

        check(res, {
            'transaction first page status is 200': (r) => r.status === 200,
            'transaction first page has content array': (r) => {
                try {
                    return Array.isArray(r.json('content'));
                } catch (e) {
                    return false;
                }
            },
        });
    });

    group('transaction history deep page', () => {
        const pageNo = Math.floor(Math.random() * 50);

        const res = http.get(
            `${BASE_URL}/accounts/${USER_ACCOUNT_1}/transactions?pageNo=${pageNo}&pageSize=20`,
            {
                ...jsonHeaders(token),
                tags: { name: 'transaction_history_deep_page' },
            }
        );

        if (res.status !== 200) {
            console.error(`transaction deep page failed. pageNo=${pageNo}, status=${res.status}, body=${res.body}`);
        }

        check(res, {
            'transaction deep page status is 200': (r) => r.status === 200,
            'transaction deep page has content array': (r) => {
                try {
                    return Array.isArray(r.json('content'));
                } catch (e) {
                    return false;
                }
            },
        });
    });

    sleep(1);
}