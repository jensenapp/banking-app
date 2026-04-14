import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { register } from "../services/AuthService";

export default function Signup() {
  const navigate = useNavigate();

  // 1. 為所有表單欄位建立本地 State
  const [username, setUsername] = useState("");
  const [realName, setRealName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  // 錯誤提示與載入狀態
  const [passwordError, setPasswordError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 2. 傳統的表單送出處理函式
  const handleFormSubmit = async (e) => {
    e.preventDefault(); // 阻止表單預設的重整行為

    // 前端攔截密碼不一致的錯誤
    if (password !== confirmPassword) {
      setPasswordError("兩次輸入的密碼不一致！");
      return; // 密碼不一致就直接中斷，不發送 API
    } else {
      setPasswordError("");
    }

    setIsSubmitting(true); // 開啟載入狀態，防止連點

    // 依照後端 SignupRequest DTO 的格式準備 Payload
    const signupData = {
      username,
      email,
      password,
      realName,
      role: ["USER"], // 後端 DTO 要求是 Set<String>，傳陣列過去會自動轉型
    };

    try {
      // 呼叫註冊 API
      await register(signupData);
      
      // 成功處理
      toast.success("註冊成功！請使用新帳號登入系統。");
      navigate("/login");
    } catch (error) {
      // 失敗處理：攔截後端拋出的 400 錯誤 (例如: Username is already taken!)
      if (error.response?.status === 400) {
        const errorMessage = error.response.data.message || error.response.data;
        toast.error(typeof errorMessage === 'string' ? errorMessage : "輸入格式不正確或帳號已存在");
      } else {
        toast.error("伺服器連線異常，請稍後再試。");
      }
    } finally {
      setIsSubmitting(false); // 無論成功失敗，都要關閉載入狀態
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
          {/* 改回標準的 HTML <form> */}
          <form className="space-y-5" onSubmit={handleFormSubmit}>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              
              {/* 使用者帳號 */}
              <div className="sm:col-span-2">
                <label htmlFor="username" className={labelStyle}>使用者帳號 (Username)</label>
                <input 
                  id="username" type="text" required minLength={3} maxLength={20} 
                  value={username} onChange={(e) => setUsername(e.target.value)} // 新增雙向綁定
                  className={inputStyle} placeholder="請輸入 3-20 個字元" 
                />
              </div>

              {/* 真實姓名 */}
              <div>
                <label htmlFor="realName" className={labelStyle}>真實姓名 (Real Name)</label>
                <input 
                  id="realName" type="text" required minLength={2} 
                  value={realName} onChange={(e) => setRealName(e.target.value)} // 新增雙向綁定
                  className={inputStyle} placeholder="例如：王小明" 
                />
              </div>

              {/* 電子信箱 */}
              <div>
                <label htmlFor="email" className={labelStyle}>電子信箱 (Email)</label>
                <input 
                  id="email" type="email" required 
                  value={email} onChange={(e) => setEmail(e.target.value)} // 新增雙向綁定
                  className={inputStyle} placeholder="example@email.com" 
                />
              </div>

              {/* 密碼 */}
              <div>
                <label htmlFor="password" className={labelStyle}>密碼 (Password)</label>
                <input 
                  id="password" type="password" required minLength={6} 
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
          </form>
        </div>
      </div>
    </div>
  );
}