import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import {
    BASE_URL,
    USERNAME,
    PASSWORD,
    USER_ACCOUNT_1,
    USER_ACCOUNT_2,
    login,
    jsonHeaders,
    uuidLike,
} from './common.js';

const AMOUNT = Number(__ENV.AMOUNT || 1);

// 因為 duplicate request 預期可能回 400，所以把 400 視為「expected response」
// 否則 http_req_failed 會把大量 duplicate 400 算成 HTTP failure。
http.setResponseCallback(
    http.expectedStatuses({ min: 200, max: 399 }, 400)
);

export const normalTransfer200 = new Counter('normal_transfer_200');
export const normalTransfer400 = new Counter('normal_transfer_400');

export const duplicateTransfer200 = new Counter('duplicate_transfer_200');
export const duplicateTransfer400 = new Counter('duplicate_transfer_400');

export const serverErrorRate = new Rate('server_error_rate');
export const unexpectedStatusRate = new Rate('unexpected_status_rate');

export const options = {
    scenarios: {
        normal_transfer: {
            executor: 'constant-vus',
            vus: 5,
            duration: '30s',
            exec: 'normalTransfer',
        },
        duplicate_transfer: {
            executor: 'shared-iterations',
            vus: 10,
            iterations: 20,
            exec: 'duplicateTransfer',
            startTime: '40s',
        },
    },
    thresholds: {
        checks: ['rate>0.90'],
        http_req_failed: ['rate<0.01'],

        server_error_rate: ['rate==0'],
        unexpected_status_rate: ['rate<0.01'],

        // 同一個 idempotencyKey 理論上最多只能成功一次
        duplicate_transfer_200: ['count<=1'],

        'http_req_duration{name:normal_transfer}': ['p(95)<1500', 'p(99)<3000'],
        'http_req_duration{name:duplicate_transfer}': ['p(95)<1500', 'p(99)<3000'],
    },
};

export function setup() {
    const userToken = login(USERNAME, PASSWORD);

    return {
        userToken,
        duplicateKey: `duplicate-transfer-${Date.now()}`,
    };
}

function recordResult(operation, res) {
    const isExpectedStatus = res.status === 200 || res.status === 400;

    serverErrorRate.add(res.status >= 500);
    unexpectedStatusRate.add(!isExpectedStatus);

    if (!isExpectedStatus) {
        console.error(`${operation} unexpected. status=${res.status}, body=${res.body}`);
    }

    if (res.status >= 500) {
        console.error(`${operation} server error. status=${res.status}, body=${res.body}`);
    }
}

export function normalTransfer(data) {
    group('normal transfer - unique idempotency key', () => {
        const res = http.post(
            `${BASE_URL}/accounts/transfer`,
            JSON.stringify({
                fromAccountId: USER_ACCOUNT_1,
                toAccountId: USER_ACCOUNT_2,
                amount: AMOUNT,
                idempotencyKey: uuidLike(),
            }),
            {
                ...jsonHeaders(data.userToken),
                tags: { name: 'normal_transfer' },
            }
        );

        recordResult('normal transfer', res);

        if (res.status === 200) {
            normalTransfer200.add(1);
        }

        if (res.status === 400) {
            normalTransfer400.add(1);
            console.error(`normal transfer business 400. body=${res.body}`);
        }

        check(res, {
            'normal transfer status is 200 or business 400': (r) => r.status === 200 || r.status === 400,
            'normal transfer no 500': (r) => r.status < 500,
        });
    });

    sleep(0.3);
}

export function duplicateTransfer(data) {
    group('duplicate transfer - same idempotency key', () => {
        const res = http.post(
            `${BASE_URL}/accounts/transfer`,
            JSON.stringify({
                fromAccountId: USER_ACCOUNT_1,
                toAccountId: USER_ACCOUNT_2,
                amount: AMOUNT,
                idempotencyKey: data.duplicateKey,
            }),
            {
                ...jsonHeaders(data.userToken),
                tags: { name: 'duplicate_transfer' },
            }
        );

        recordResult('duplicate transfer', res);

        if (res.status === 200) {
            duplicateTransfer200.add(1);
        }

        if (res.status === 400) {
            duplicateTransfer400.add(1);
        }

        check(res, {
            'duplicate transfer status is 200 or duplicate 400': (r) => r.status === 200 || r.status === 400,
            'duplicate transfer no 500': (r) => r.status < 500,
        });
    });

    sleep(0.1);
}