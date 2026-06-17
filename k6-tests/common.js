import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8089/api';

export const USERNAME = __ENV.USERNAME || 'user1';
export const PASSWORD = __ENV.PASSWORD || 'password1';

export const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
export const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'adminPass';

export const USER_ACCOUNT_1 = Number(__ENV.USER_ACCOUNT_1 || 1);
export const USER_ACCOUNT_2 = Number(__ENV.USER_ACCOUNT_2 || 2);
export const ADMIN_TEST_ACCOUNT = Number(__ENV.ADMIN_TEST_ACCOUNT || USER_ACCOUNT_1);

export function jsonHeaders(token) {
    const headers = {
        'Content-Type': 'application/json',
        Accept: 'application/json',
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    return { headers };
}

export function login(username, password) {
    const res = http.post(
        `${BASE_URL}/auth/public/signin`,
        JSON.stringify({ username, password }),
        jsonHeaders()
    );

    const ok = check(res, {
        'login status is 200': (r) => r.status === 200,
        'login response has jwtToken': (r) => {
            try {
                return !!r.json('jwtToken');
            } catch (e) {
                return false;
            }
        },
    });

    if (!ok) {
        console.error(`Login failed. status=${res.status}, body=${res.body}`);
        fail('Login failed');
    }

    return res.json('jwtToken');
}

export function uuidLike() {
    return `${Date.now()}-${__VU}-${__ITER}-${Math.random().toString(16).slice(2)}`;
}