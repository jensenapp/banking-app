import React from 'react';
import { toast } from 'react-toastify';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from "../store/auth-context";

export default function HeaderComponent() {
  const navigate = useNavigate();
  
  // 1. 新增解構出 roles (角色陣列)
  const { isAuthenticated, username, roles, logout } = useAuth();

  // 2. 判斷當前登入者是否包含管理員權限
  const isAdmin = roles && roles.includes("ROLE_ADMIN");

  const handleLogout = () => {
    logout();
    toast.success("成功登出系統");
    navigate("/login");
  };

  return (
    <header className="sticky top-0 z-50">
      <nav className="bg-blue-900 px-6 py-4 shadow-md flex items-center justify-between">
        <Link to="/" className="text-xl font-bold tracking-tight text-white hover:text-blue-200 transition">
          Banking App
        </Link>
        
        {isAuthenticated ? (
          <div className="flex items-center gap-4 text-white">
            <span className="font-medium hidden sm:inline-block">嗨，{username}</span>
            
            {/* 3. 動態渲染：如果是管理員，才顯示「管理後台」按鈕 */}
            {isAdmin && (
              <Link 
                to="/admin" 
                className="text-sm bg-indigo-600 hover:bg-indigo-500 px-3 py-1.5 rounded transition font-medium border border-indigo-400 shadow-sm"
              >
                管理後台
              </Link>
            )}

            <button
              onClick={handleLogout}
              className="bg-red-500 text-white px-4 py-1.5 rounded-md hover:bg-red-600 transition shadow"
            >
              登出
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-4">
            <Link to="/login" className="text-white hover:text-blue-200 font-medium transition">
              登入
            </Link>
            {/* 4. 補上未登入狀態的「註冊開戶」按鈕 */}
            <Link 
              to="/signup" 
              className="bg-white text-blue-900 px-4 py-1.5 rounded-md hover:bg-gray-100 transition font-medium shadow-sm"
            >
              註冊開戶
            </Link>
          </div>
        )}
      </nav>
    </header>
  );
}