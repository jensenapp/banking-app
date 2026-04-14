import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getUserProfile } from '../services/UserService';
import { getMyAccounts } from '../services/AccountService';

export default function Dashboard() {
  const [profile, setProfile] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [isPageLoading, setIsPageLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setIsPageLoading(true);
    try {
      const [profileRes, accountsRes] = await Promise.all([
        getUserProfile(),
        getMyAccounts()
      ]);
      setProfile(profileRes.data);
      setAccounts(accountsRes.data);
    } catch (error) {
      toast.error("無法取得資料，請重新登入或稍後再試。");
    } finally {
      setIsPageLoading(false);
    }
  };

  if (isPageLoading) {
    return <div className="p-8 text-center text-xl font-bold text-blue-800">資料載入中...</div>;
  }

  return (
    // 1. 稍微放寬外層容器到 max-w-6xl
    <div className="p-4 sm:p-8 max-w-6xl mx-auto w-full">
      <h1 className="text-3xl font-extrabold mb-6 text-gray-900">帳戶總覽</h1>

      {/* 2. 響應式 Flexbox：手機時上下排 (flex-col)，大螢幕時左右排 (lg:flex-row) */}
      <div className="flex flex-col lg:flex-row gap-8">
        
        {/* 左邊：個人資料 (大螢幕佔 1/4 寬度) */}
        <div className="w-full lg:w-1/4 bg-white p-6 border border-gray-200 rounded-xl shadow-sm h-fit">
          <h2 className="text-xl font-bold mb-4 border-b pb-3 text-gray-800">個人資料</h2>
          
          <div className="space-y-4 text-gray-700">
            <p className="flex flex-col">
              <span className="text-sm text-gray-500 font-semibold mb-1">帳號</span>
              <span className="font-medium text-lg">{profile?.username}</span>
            </p>
            <p className="flex flex-col">
              <span className="text-sm text-gray-500 font-semibold mb-1">信箱</span>
              {/* break-all 防止信箱太長撐破版面 */}
              <span className="font-medium break-all">{profile?.email}</span>
            </p>
            <p className="flex flex-col">
              <span className="text-sm text-gray-500 font-semibold mb-1">系統權限</span>
              <span className="font-medium bg-blue-100 text-blue-800 w-fit px-2 py-0.5 rounded text-sm">
                {profile?.roles?.join(', ')}
              </span>
            </p>
          </div>
        </div>

        {/* 右邊：帳戶列表 (大螢幕時使用 flex-1 填滿剩餘的 3/4 空間) */}
        <div className="w-full lg:flex-1">
          {accounts.length === 0 ? (
            <div className="bg-white p-10 text-center rounded-xl border border-gray-200 shadow-sm text-gray-600">
              <span className="text-4xl block mb-4">🏦</span>
              您目前沒有帳戶，請攜帶證件至實體分行辦理。
            </div>
          ) : (
            // 3. 響應式 Grid：手機 1 欄，平板 2 欄，特大螢幕 3 欄
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
              {accounts.map(acc => (
                <div key={acc.id} className="bg-blue-800 text-white p-6 rounded-2xl shadow-md hover:shadow-lg transition-shadow flex flex-col justify-between">
                  
                  {/* 卡片上半部：資訊 */}
                  <div>
                    <div className="flex justify-between items-center mb-2">
                      <span className="font-medium text-blue-100">{acc.accountHolderName}</span>
                      <span className="text-xs bg-blue-700 px-2 py-1 rounded border border-blue-600 font-mono">
                        ID: {acc.id}
                      </span>
                    </div>
                    <div className="text-3xl font-extrabold my-4 tracking-tight">
                      $ {acc.balance.toLocaleString()}
                    </div>
                  </div>
                  
                  {/* 卡片下半部：操作按鈕 */}
                  <div className="mt-4 space-y-3">
                    <Link 
                      to={`/accounts/${acc.id}/transfer`}
                      className="block text-center w-full bg-white text-blue-800 py-2.5 rounded-lg font-bold hover:bg-gray-100 transition shadow-sm"
                    >
                      轉帳
                    </Link>
                    
                    <Link 
                      to={`/accounts/${acc.id}/transactions`} 
                      className="block text-center w-full text-sm text-blue-200 hover:text-white py-1 transition underline-offset-4 hover:underline"
                    >
                      查看明細
                    </Link>
                  </div>

                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}