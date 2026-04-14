import React, { useState } from "react";
import { Link, useNavigate, useLocation, Navigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../store/auth-context";
import { login } from "../services/AuthService";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { loginSuccess } = useAuth();
  
  // 1. 本地狀態管理
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  


  // 2. 傳統的表單提交處理函式
  const handleLogin = async (e) => {
    e.preventDefault(); // 阻止畫面重整
    setIsSubmitting(true);

    try {
      // 呼叫 API
      const response = await login({ username, password });
      const { jwtToken, username: resUser, roles } = response.data;

      // 更新 Context 狀態
      loginSuccess(jwtToken, resUser, roles);
      
      toast.success(`登入成功！歡迎回來，${resUser}。`);


    if (roles.includes("ROLE_ADMIN")) {
      return navigate("/admin",{ replace: true });
    }else{
      return navigate("/dashboard",{ replace: true });
    }
  
   
     

    } catch (error) {
      if (error.response?.status === 401) {
        toast.error("帳號或密碼錯誤，請重新輸入！");
      } else {
        toast.error(error.response?.data?.message || "伺服器連線異常，請稍後再試。");
      }
    } finally {
      setIsSubmitting(false); // 無論成功失敗，解除按鈕禁用
    }
  };

  const labelStyle = "block text-lg font-semibold text-blue-900 mb-2";
  const textFieldStyle = "w-full px-4 py-2 text-base border rounded-md transition border-gray-300 focus:ring focus:ring-blue-500 focus:outline-none text-gray-800 bg-white placeholder-gray-400 shadow-sm";

  return (
    <div className="min-h-[85vh] flex items-center justify-center bg-gray-100 py-12 px-4">
      <div className="bg-white shadow-xl border border-gray-200 rounded-2xl max-w-md w-full px-8 py-10">
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-blue-900 tracking-wide">歡迎登入</h2>
        </div>

        {/* 改回標準的 html form */}
        <form onSubmit={handleLogin} className="space-y-6">
          <div>
            <label className={labelStyle}>使用者帳號</label>
            <input
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)} // 雙向綁定
              className={textFieldStyle}
            />
          </div>

          <div>
            <label className={labelStyle}>密碼</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)} // 雙向綁定
              className={textFieldStyle}
            />
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full px-6 py-3 text-white text-lg font-bold rounded-lg transition duration-200 bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? "身分驗證中..." : "登入系統"}
            </button>
          </div>
        </form>

        <p className="text-center text-gray-600 mt-6">
          還沒有網路銀行帳戶？{" "}
          <Link to="/signup" className="text-blue-600 hover:text-blue-800 font-bold">立即註冊</Link>
        </p>
      </div>
    </div>
  );
}