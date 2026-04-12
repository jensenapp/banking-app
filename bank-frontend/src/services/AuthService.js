import apiClient from "../api/apiClient";

const BASE = "/auth/public"; // 對應你的後端 AuthController 路徑

export const login = (data) => apiClient.post(`${BASE}/signin`, data);
export const register = (data) => apiClient.post(`${BASE}/signup`, data);