import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
    BASE_URL,
    ADMIN_USERNAME,
    ADMIN_PASSWORD,
    ADMIN_TEST_ACCOUNT,
    login,
    jsonHeaders,
} from './common.js';

const AMOUNT = Number(__ENV.AMOUNT || 1);

export const depositSuccess = new Counter('deposit_success');
export const withdrawSuccess = new Counter('withdraw_success');
export const moneyOpFailed = new Rate('money_op_failed');

export const options = {
    stages: [
        { duration: '20s', target: 10 },
        { duration: '40s', target: 20 },
        { duration: '20s', target: 0 },
    ],
    thresholds: {
        checks: ['rate>0.95'],
        http_req_failed: ['rate<0.05'],
        money_op_failed: ['rate<0.01'],

        'http_req_duration{name:deposit}': ['p(95)<1000', 'p(99)<2500'],
        'http_req_duration{name:withdraw}': ['p(95)<1000', 'p(99)<2500'],
    },
};

export function setup() {
    const adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
    return { adminToken };
}

function logFailure(operation, res) {
    if (res.status !== 200) {
        console.error(
            `${operation} failed. status=${res.status}, body=${res.body}`
        );
    }
}

export default function (data) {
    const token = data.adminToken;

    const shouldDeposit = (__ITER + __VU) % 2 === 0;

    if (shouldDeposit) {
        group('deposit concurrency', () => {
            const res = http.put(
                `${BASE_URL}/accounts/${ADMIN_TEST_ACCOUNT}/deposit`,
                JSON.stringify({ amount: AMOUNT }),
                {
                    ...jsonHeaders(token),
                    tags: { name: 'deposit' },
                }
            );

            logFailure('deposit', res);

            const ok = check(res, {
                'deposit status is 200': (r) => r.status === 200,
            });

            moneyOpFailed.add(!ok);

            if (res.status === 200) {
                depositSuccess.add(1);
            }
        });
    } else {
        group('withdraw concurrency', () => {
            const res = http.put(
                `${BASE_URL}/accounts/${ADMIN_TEST_ACCOUNT}/withdraw`,
                JSON.stringify({ amount: AMOUNT }),
                {
                    ...jsonHeaders(token),
                    tags: { name: 'withdraw' },
                }
            );

            logFailure('withdraw', res);

            const ok = check(res, {
                'withdraw status is 200': (r) => r.status === 200,
            });

            moneyOpFailed.add(!ok);

            if (res.status === 200) {
                withdrawSuccess.add(1);
            }
        });
    }

    sleep(0.2);
}