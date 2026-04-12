import { useAuth } from "../store/auth-context";
import { Navigate, Outlet, useLocation } from "react-router-dom";

export default function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation(); // 取得當前網址
  
  if (!isAuthenticated) {
    // 使用者沒登入卻想看需要權限的頁面，把當前網址記下來
    sessionStorage.setItem("redirectPath", location.pathname);
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}