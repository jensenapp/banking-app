import apiClient from "../api/apiClient";

const BASE = "/auth";

// 取得當前登入使用者的詳細資訊
export const getUserProfile = () => apiClient.get(`${BASE}/user`);