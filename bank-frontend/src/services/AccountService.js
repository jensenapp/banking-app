import apiClient from "../api/apiClient";

const BASE = "/accounts"; // 對應你的後端 AccountController 路徑

export const getAccount = (id) => apiClient.get(`${BASE}/${id}`);
export const deposit = (id, data) => apiClient.put(`${BASE}/${id}/deposit`, data);
export const withdraw = (id, data) => apiClient.put(`${BASE}/${id}/withdraw`, data);
export const transfer = (data) => apiClient.post(`${BASE}/transfer`, data);
export const createAccount = (data) => apiClient.post(BASE, data);
export const getMyAccounts = () => apiClient.get(`${BASE}/my-accounts`);


export const getTransactions = (id, pageNo = 0, pageSize = 3) => 
  apiClient.get(`${BASE}/${id}/transactions?pageNo=${pageNo}&pageSize=${pageSize}`);


export const getAllAccounts = (pageNo = 0, pageSize = 5, sortBy = 'id', sortDir = 'asc') => 
  apiClient.get(`${BASE}?pageNo=${pageNo}&pageSize=${pageSize}&sortBy=${sortBy}&sortDir=${sortDir}`);

export const deleteAccount = (id) => apiClient.delete(`${BASE}/${id}`);