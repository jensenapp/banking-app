import React, { useEffect } from "react";
import { Link, Form, useActionData, useNavigation, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../store/auth-context"; // 使用 Context API 而不是 Redux
import { login } from "../services/AuthService";

export default function Login() {
  const actionData = useActionData();
  const navigation = useNavigation();
  const isSubmitting = navigation.state === "submitting";
  const navigate = useNavigate();
  
  const { loginSuccess } = useAuth();
  
  // 智慧導航：獲取登入前被攔截的路徑，預設為 /dashboard
  const from = sessionStorage.getItem("redirectPath") || "/dashboard";

  useEffect(() => {
    if (actionData?.success) {
      // 1. 更新 Context 全域狀態
      loginSuccess(actionData.jwtToken, actionData.username, actionData.roles);
      
      // 2. 清除暫存的路徑
      sessionStorage.removeItem("redirectPath");
      toast.success(`登入成功！歡迎回來，${actionData.username}。`);

      // 3. 判斷權限決定跳轉目的地：管理員強制去後台，一般使用者去 from
      const targetPath = actionData.roles.includes("ROLE_ADMIN") ? "/admin" : from;

      // 4. 延遲跳轉確保狀態寫入
      setTimeout(() => {
        navigate(targetPath, { replace: true });
      }, 100);

    } else if (actionData?.errors) {
      toast.error(actionData.errors.message || "登入失敗，請檢查帳號密碼");
    }
  }, [actionData, loginSuccess, navigate, from]);

  // 共用樣式
  const labelStyle = "block text-lg font-semibold text-blue-900 mb-2";
  const textFieldStyle = "w-full px-4 py-2 text-base border rounded-md transition border-gray-300 focus:ring focus:ring-blue-500 focus:outline-none text-gray-800 bg-white placeholder-gray-400 shadow-sm";

  return (
    <div className="min-h-[85vh] flex items-center justify-center bg-gray-100 py-12 px-4">
      <div className="bg-white shadow-xl border border-gray-200 rounded-2xl max-w-md w-full px-8 py-10">
        
        {/* Title */}
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-blue-900 tracking-wide">歡迎登入</h2>
          <p className="text-gray-500 mt-2">請輸入您的帳號密碼以繼續</p>
        </div>

        {/* Form */}
        <Form method="POST" className="space-y-6">
          {/* Username Field */}
          <div>
            <label htmlFor="username" className={labelStyle}>
              使用者帳號 (Username)
            </label>
            <input
              id="username"
              type="text"
              name="username"
              placeholder="請輸入您的帳號"
              autoComplete="username"
              required
              className={textFieldStyle}
            />
          </div>

          {/* Password Field */}
          <div>
            <label htmlFor="password" className={labelStyle}>
              密碼 (Password)
            </label>
            <input
              id="password"
              type="password"
              name="password"
              placeholder="請輸入您的密碼"
              autoComplete="current-password"
              required
              minLength={4}
              maxLength={20}
              className={textFieldStyle}
            />
          </div>

          {/* Submit Button */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full px-6 py-3 text-white text-lg font-bold rounded-lg transition duration-200 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed shadow-md"
            >
              {isSubmitting ? "身分驗證中..." : "登入系統"}
            </button>
          </div>
        </Form>

        {/* Register Link */}
        <p className="text-center text-gray-600 mt-6">
          還沒有網路銀行帳戶？{" "}
          <Link
            to="/signup"
            className="text-blue-600 hover:text-blue-800 font-bold transition duration-200"
          >
            立即註冊
          </Link>
        </p>
      </div>
    </div>
  );
}

// ==========================================
// Action Function (對接 Spring Boot 後端)
// ==========================================
export async function loginAction({ request }) {
  const data = await request.formData();

  const loginData = {
    username: data.get("username"),
    password: data.get("password"),
  };

  try {
    // 呼叫我們寫好的 AuthService，對應後端 POST /api/auth/public/signin
    const response = await login(loginData);
    
    // 配合 Spring Boot 後端 LoginResponse 的 DTO 結構提取資料
    const { jwtToken, username, roles } = response.data;
    
    return { success: true, jwtToken, username, roles };
  } catch (error) {
    if (error.response?.status === 401) {
      return {
        success: false,
        errors: { message: "帳號或密碼錯誤，請重新輸入！" },
      };
    }
    throw new Response(
      error.response?.data?.message || "伺服器連線異常，請稍後再試。",
      { status: error.response?.status || 500 }
    );
  }
}