import React, { useEffect, useState } from "react";
import { Form, useActionData, useNavigation, useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { register } from "../services/AuthService";

export default function Signup() {
  const actionData = useActionData();
  const navigation = useNavigation();
  const navigate = useNavigate();

  const isSubmitting = navigation.state === "submitting";

  // 用本地 state 來處理「確認密碼」的即時比對，提升 UX
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");

  // 監聽後端回傳結果
  useEffect(() => {
    if (actionData?.success) {
      toast.success("註冊成功！請使用新帳號登入系統。");
      navigate("/login");
    } else if (actionData?.errors) {
      toast.error(actionData.errors.message || "註冊失敗，請檢查輸入資料");
    }
  }, [actionData, navigate]);

  // 在表單送出前，先在前端攔截密碼不一致的錯誤
  const handleFormSubmit = (e) => {
    if (password !== confirmPassword) {
      e.preventDefault(); // 阻止表單送出給 Action
      setPasswordError("兩次輸入的密碼不一致！");
    } else {
      setPasswordError("");
    }
  };

  const labelStyle = "block text-sm font-semibold text-gray-700 mb-1";
  const inputStyle = "w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-gray-50 text-gray-900";

  return (
    <div className="flex flex-col items-center justify-center min-h-[85vh] bg-gray-100 py-12 px-4">
      <div className="max-w-lg w-full bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden">
        <div className="bg-blue-900 py-6 px-8 text-center">
          <h2 className="text-2xl font-bold text-white tracking-wide">開立網路銀行帳戶</h2>
          <p className="text-blue-200 text-sm mt-2">只需幾分鐘，立即享受便捷的金融服務</p>
        </div>

        <div className="p-8">
          <Form method="POST" className="space-y-5" onSubmit={handleFormSubmit}>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              {/* 使用者帳號 */}
              <div className="sm:col-span-2">
                <label htmlFor="username" className={labelStyle}>使用者帳號 (Username)</label>
                <input id="username" name="username" type="text" required minLength={3} maxLength={20} className={inputStyle} placeholder="請輸入 3-20 個字元" />
              </div>

              {/* 真實姓名 */}
              <div>
                <label htmlFor="realName" className={labelStyle}>真實姓名 (Real Name)</label>
                <input id="realName" name="realName" type="text" required minLength={2} className={inputStyle} placeholder="例如：王小明" />
              </div>

              {/* 電子信箱 */}
              <div>
                <label htmlFor="email" className={labelStyle}>電子信箱 (Email)</label>
                <input id="email" name="email" type="email" required className={inputStyle} placeholder="example@email.com" />
              </div>

              {/* 密碼 */}
              <div>
                <label htmlFor="password" className={labelStyle}>密碼 (Password)</label>
                <input 
                  id="password" name="password" type="password" required minLength={6} 
                  value={password} onChange={(e) => setPassword(e.target.value)}
                  className={inputStyle} placeholder="至少 6 個字元" 
                />
              </div>

              {/* 確認密碼 */}
              <div>
                <label htmlFor="confirmPassword" className={labelStyle}>確認密碼</label>
                <input 
                  id="confirmPassword" type="password" required minLength={6}
                  value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)}
                  className={`${inputStyle} ${passwordError ? 'border-red-500 focus:ring-red-500' : ''}`} placeholder="請再次輸入密碼" 
                />
              </div>
            </div>

            {/* 密碼錯誤提示 */}
            {passwordError && <p className="text-red-500 text-sm font-medium">{passwordError}</p>}

            <div className="pt-4">
              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full flex justify-center py-3 px-4 rounded-lg text-lg font-medium text-white bg-blue-600 hover:bg-blue-700 transition disabled:opacity-50"
              >
                {isSubmitting ? "送出資料中..." : "立即註冊"}
              </button>
            </div>

            <div className="text-center mt-4">
              <span className="text-gray-500 text-sm">已經有帳戶了嗎？ </span>
              <Link to="/login" className="text-blue-600 hover:text-blue-800 font-medium text-sm transition">
                前往登入
              </Link>
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
}

// ==========================================
// Action Function (處理註冊 API 請求)
// ==========================================
export async function signupAction({ request }) {
  const data = await request.formData();
  
  // 依照後端 SignupRequest DTO 的格式準備 Payload
  const signupData = {
    username: data.get("username"),
    email: data.get("email"),
    password: data.get("password"),
    realName: data.get("realName"),
    role: ["USER"] // 後端 DTO 要求是 Set<String>，傳陣列過去會自動轉型
  };

  try {
    await register(signupData);
    return { success: true };
  } catch (error) {
    // 攔截後端拋出的 400 錯誤 (例如: Username is already taken!)
    if (error.response?.status === 400) {
      // 處理 @Valid 驗證錯誤陣列，或是自訂的 MessageResponse
      const errorMessage = error.response.data.message || error.response.data;
      return { success: false, errors: { message: typeof errorMessage === 'string' ? errorMessage : "輸入格式不正確或帳號已存在" } };
    }
    throw new Response("伺服器連線異常，請稍後再試。", { status: 500 });
  }
}