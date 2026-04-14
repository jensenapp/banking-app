import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../store/auth-context';

export default function Home() {
  // 從 Context 取得登入狀態，用來決定要顯示什麼按鈕
  const { isAuthenticated,roles } = useAuth();

  const isAdmin=roles.includes("ROLE_ADMIN");

  return (
    <div className="flex flex-col min-h-[85vh] bg-white">
      
      {/* ========================================== */}
      {/* 英雄區塊 (Hero Section) */}
      {/* ========================================== */}
      <section className="relative bg-gradient-to-br from-blue-900 via-indigo-800 to-blue-900 text-white py-20 lg:py-32 overflow-hidden flex-grow flex items-center justify-center">
        {/* 背景裝飾光暈 */}
        <div className="absolute top-0 left-1/2 transform -translate-x-1/2 w-[800px] h-[400px] bg-white opacity-5 rounded-full blur-3xl pointer-events-none"></div>
        
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center z-10 w-full">
          <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight mb-6">
            新世代數位金融，<span className="text-blue-300">為您而生</span>
          </h1>
          <p className="mt-4 text-xl md:text-2xl text-blue-100 max-w-3xl mx-auto mb-12 font-light tracking-wide">
            體驗極致流暢的存提款與轉帳服務。零時差、零距離，隨時隨地掌控您的財富藍圖。
          </p>
          
          <div className="flex flex-col sm:flex-row justify-center items-center gap-4">
            {isAuthenticated ? (
              // 已登入狀態的按鈕
              <Link 
                to={isAdmin ? "/admin" :"/dashboard" }
                className="w-full sm:w-auto bg-white text-blue-900 font-bold px-8 py-3.5 rounded-full hover:bg-blue-50 transition-all shadow-lg hover:shadow-xl text-lg transform hover:-translate-y-1"
              >
                {isAdmin ? "進入後臺 ➔": "進入帳戶總覽"}
              </Link>
            ) : (
              // 未登入狀態的按鈕
              <>
                <Link 
                  to="/signup" 
                  className="w-full sm:w-auto bg-blue-500 text-white font-bold px-8 py-3.5 rounded-full hover:bg-blue-400 transition-all shadow-lg hover:shadow-xl text-lg transform hover:-translate-y-1"
                >
                  免費開戶
                </Link>
                <Link 
                  to="/login" 
                  className="w-full sm:w-auto bg-transparent border-2 border-blue-200 text-blue-100 font-bold px-8 py-3.5 rounded-full hover:bg-blue-800 hover:border-white hover:text-white transition-all shadow-lg text-lg"
                >
                  立即登入
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

    </div>
  );
}