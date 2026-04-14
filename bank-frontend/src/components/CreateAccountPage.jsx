// src/components/CreateAccountPage.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createAccount } from '../services/AccountService';

export default function CreateAccountPage() {
  const navigate = useNavigate();
  
  const [targetUserId, setTargetUserId] = useState('');
  const [initialBalance, setInitialBalance] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    const balance = Number(initialBalance);
    const userId = Number(targetUserId);

    if (!userId) return toast.warn("請輸入目標客戶 ID");
    if (balance < 0) return toast.warn("初始餘額不能為負數");

    setIsProcessing(true);
    try {
      await createAccount({ userId: userId, balance: balance });
      toast.success(`成功為客戶 ID: ${userId} 開立新帳戶！`);
      navigate('/admin'); // 開戶成功後導回管理列表
    } catch (error) {
      toast.error(error.response?.data?.message || "開戶失敗，請檢查客戶 ID 是否正確。");
      setIsProcessing(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-10 w-full">
      <div className="bg-white rounded-xl shadow-lg overflow-hidden border border-gray-200">
        <div className="bg-indigo-600 px-6 py-4">
          <h2 className="text-xl font-bold text-white">替客戶開立新帳戶</h2>
        </div>
        
        <form onSubmit={handleCreateSubmit} className="p-8 space-y-6">
          <div className="bg-indigo-50 p-5 rounded-lg border border-indigo-100 mb-6">
            <p className="text-sm text-indigo-800">
              請確認客戶已經在系統中註冊，並準備好他們的使用者 ID (User ID) 以綁定新帳戶。
            </p>
          </div>

          <div>
            <label className="block text-base font-semibold text-gray-700 mb-2">目標客戶 User ID</label>
            <input 
              type="number" required min="1" 
              value={targetUserId} onChange={e => setTargetUserId(e.target.value)} 
              className="w-full border-2 border-indigo-200 focus:border-indigo-500 focus:ring-0 p-4 rounded-lg text-lg" 
              placeholder="例如: 1" 
            />
          </div>
          
          <div>
            <label className="block text-base font-semibold text-gray-700 mb-2">初始存入餘額 (TWD)</label>
            <input 
              type="number" required min="0" 
              value={initialBalance} onChange={e => setInitialBalance(e.target.value)} 
              className="w-full border-2 border-indigo-200 focus:border-indigo-500 focus:ring-0 p-4 rounded-lg text-lg" 
              placeholder="0" 
            />
          </div>
          
          <div className="flex gap-4 pt-4">
            <button type="button" onClick={() => navigate('/admin')} className="flex-1 py-3 text-gray-600 font-bold bg-gray-100 rounded-lg hover:bg-gray-200 transition">取消返回</button>
            <button type="submit" disabled={isProcessing} className="flex-[2] bg-indigo-600 text-white py-3 rounded-lg font-bold hover:bg-indigo-700 disabled:bg-indigo-400 transition">確認開戶</button>
          </div>
        </form>
      </div>
    </div>
  );
}