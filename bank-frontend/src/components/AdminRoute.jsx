import React from "react";
import { useAuth } from "../store/auth-context";
import { Navigate, Outlet } from "react-router-dom";
import { toast } from "react-toastify";

export default function AdminRoute() {
  const { isAuthenticated, roles } = useAuth();
  
  // 檢查是否登入，且角色陣列中包含 ROLE_ADMIN
  const isAdmin = roles && roles.includes("ROLE_ADMIN");

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin) {
    toast.error("權限不足！您沒有存取管理員後台的權限。");
    return <Navigate to="/dashboard" replace />;
  }

  // 驗證通過，渲染後台組件
  return <Outlet />;
}