import http from 'k6/http';
import { check, group } from 'k6';
import {
    login,
    jsonHeaders,
    USERNAME,
    PASSWORD,
    ADMIN_USERNAME,
    ADMIN_PASSWORD,
    USER_ACCOUNT_1,
    USER_ACCOUNT_2,
    ADMIN_TEST_ACCOUNT,
    BASE_URL,
    uuidLike,
} from './common.js';

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: {
        checks: ['rate>0.95'],
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1000'],
    },
};

export default function () {
    const userToken = login(USERNAME, PASSWORD);
    const adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);

    group('account list - user', () => {
        const res = http.get(`${BASE_URL}/accounts/my-accounts`, jsonHeaders(userToken));

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

    group('transaction history - user', () => {
        const res = http.get(
            `${BASE_URL}/accounts/${USER_ACCOUNT_1}/transactions?pageNo=0&pageSize=10`,
            jsonHeaders(userToken)
        );

        if (res.status !== 200) {
            console.error(`transactions failed. status=${res.status}, body=${res.body}`);
        }

        check(res, {
            'transactions status is 200': (r) => r.status === 200,
            'transactions has content': (r) => {
                try {
                    return r.json('content') !== undefined;
                } catch (e) {
                    return false;
                }
            },
        });
    });

    group('deposit - admin', () => {
        const res = http.put(
            `${BASE_URL}/accounts/${ADMIN_TEST_ACCOUNT}/deposit`,
            JSON.stringify({ amount: 1 }),
            jsonHeaders(adminToken)
        );

        if (res.status !== 200) {
            console.error(`deposit failed. status=${res.status}, body=${res.body}`);
        }

        check(res, {
            'deposit status is 200': (r) => r.status === 200,
        });
    });

    group('withdraw - admin', () => {
        const res = http.put(
            `${BASE_URL}/accounts/${ADMIN_TEST_ACCOUNT}/withdraw`,
            JSON.stringify({ amount: 1 }),
            jsonHeaders(adminToken)
        );

        if (res.status !== 200 && res.status !== 400) {
            console.error(`withdraw failed. status=${res.status}, body=${res.body}`);
        }

        check(res, {
            'withdraw status is 200 or insufficient amount': (r) => r.status === 200 || r.status === 400,
        });
    });

    group('transfer - user', () => {
        const res = http.post(
            `${BASE_URL}/accounts/transfer`,
            JSON.stringify({
                fromAccountId: USER_ACCOUNT_1,
                toAccountId: USER_ACCOUNT_2,
                amount: 1,
                idempotencyKey: uuidLike(),
            }),
            jsonHeaders(userToken)
        );

        if (res.status !== 200 && res.status !== 400) {
            console.error(`transfer failed. status=${res.status}, body=${res.body}`);
        }

        check(res, {
            'transfer status is 200 or business 400': (r) => r.status === 200 || r.status === 400,
        });
    });
}