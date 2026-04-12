import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../store/auth-context';

export default function Home() {
  // 從 Context 取得登入狀態，用來決定要顯示什麼按鈕
  const { isAuthenticated } = useAuth();

  return (
    <div className="flex flex-col min-h-[85vh] bg-white">
      
      {/* ========================================== */}
      {/* 英雄區塊 (Hero Section) */}
      {/* ========================================== */}
      <section className="relative bg-gradient-to-br from-blue-900 via-indigo-800 to-blue-900 text-white py-20 lg:py-32 overflow-hidden">
        {/* 背景裝飾光暈 */}
        <div className="absolute top-0 left-1/2 transform -translate-x-1/2 w-[800px] h-[400px] bg-white opacity-5 rounded-full blur-3xl pointer-events-none"></div>
        
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center z-10">
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
                to="/dashboard" 
                className="w-full sm:w-auto bg-white text-blue-900 font-bold px-8 py-3.5 rounded-full hover:bg-blue-50 transition-all shadow-lg hover:shadow-xl text-lg transform hover:-translate-y-1"
              >
                進入帳戶總覽 ➔
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

      {/* ========================================== */}
      {/* 特色介紹區塊 (Features Section) */}
      {/* ========================================== */}
      <section className="py-20 bg-gray-50 flex-grow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-extrabold text-gray-900 sm:text-4xl">
              為什麼選擇 JavaGuides Banking？
            </h2>
            <p className="mt-4 text-lg text-gray-500">
              我們提供最現代化的微服務架構，確保您的每一筆交易都安全、快速。
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
            {/* 特色卡片 1 */}
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100 hover:shadow-md transition">
              <div className="w-14 h-14 bg-blue-100 rounded-xl flex items-center justify-center mb-6">
                <span className="text-2xl">⚡</span>
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-3">極速資金周轉</h3>
              <p className="text-gray-600 leading-relaxed">
                採用最先進的 Optimistic Locking 技術處理高併發，無論是存款、提款還是跨帳戶轉帳，都在毫秒間完成，告別等待。
              </p>
            </div>

            {/* 特色卡片 2 */}
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100 hover:shadow-md transition">
              <div className="w-14 h-14 bg-blue-100 rounded-xl flex items-center justify-center mb-6">
                <span className="text-2xl">🛡️</span>
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-3">銀行級安全防護</h3>
              <p className="text-gray-600 leading-relaxed">
                全站採用 JWT (JSON Web Token) 與 Spring Security 進行無狀態身份驗證，確保您的資產與隱私滴水不漏。
              </p>
            </div>

            {/* 特色卡片 3 */}
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100 hover:shadow-md transition">
              <div className="w-14 h-14 bg-blue-100 rounded-xl flex items-center justify-center mb-6">
                <span className="text-2xl">💼</span>
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-3">多帳戶靈活管理</h3>
              <p className="text-gray-600 leading-relaxed">
                支援單一使用者開立多個虛擬子帳戶。透過直覺的現代化儀表板，輕鬆分配生活費、理財基金與夢想存款。
              </p>
            </div>
          </div>
        </div>
      </section>

    </div>
  );
}