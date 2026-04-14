// src/components/DepositPage.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAccount, deposit } from '../services/AccountService';

export default function DepositPage() {
  const { id } = useParams(); // 從網址取得帳戶 ID
  const navigate = useNavigate();
  
  const [account, setAccount] = useState(null);
  const [amount, setAmount] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  // 進入頁面時，先抓取該帳戶的詳細資訊（用來顯示持有人名稱和餘額）
  useEffect(() => {
    const fetchAccountDetails = async () => {
      try {
        const response = await getAccount(id);
        setAccount(response.data);
      } catch (error) {
        toast.error("無法取得帳戶資訊");
        navigate('/admin'); // 如果找不到帳戶，踢回管理列表
      } finally {
        setIsLoading(false);
      }
    };
    fetchAccountDetails();
  }, [id, navigate]);

  const handleDeposit = async (e) => {
    e.preventDefault();
    const numAmount = Number(amount);
    if (numAmount <= 0) return toast.warn("請輸入大於 0 的金額");

    setIsProcessing(true);
    try {
      await deposit(id, { amount: numAmount });
      toast.success(`存款成功！已存入 $${numAmount.toLocaleString()}`);
      navigate('/admin'); // 成功後導回管理列表
    } catch (error) {
      toast.error(error.response?.data?.message || "存款失敗，請稍後再試。");
      setIsProcessing(false);
    }
  };

  if (isLoading) return <div className="text-center py-20 text-xl font-bold">載入中...</div>;
  if (!account) return null;

  return (
    <div className="max-w-2xl mx-auto px-4 py-10 w-full">
      <div className="bg-white rounded-xl shadow-lg overflow-hidden border border-gray-200">
        <div className="bg-emerald-600 px-6 py-4">
          <h2 className="text-xl font-bold text-white">臨櫃存款作業</h2>
        </div>
        
        <form onSubmit={handleDeposit} className="p-8 space-y-6">
          <div className="bg-gray-50 p-5 rounded-lg border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-500 mb-2">存入帳戶資訊</h3>
            <div className="flex justify-between items-end">
              <p className="text-lg text-gray-900 font-bold">ID: {account.id} <span className="font-medium text-gray-600">({account.accountHolderName})</span></p>
              <p className="text-emerald-600 font-bold text-lg">目前餘額: ${account.balance.toLocaleString()}</p>
            </div>
          </div>
          
          <div>
            <label className="block text-base font-semibold text-gray-700 mb-2">存款金額 (TWD)</label>
            <input 
              type="number" required min="1" 
              value={amount} onChange={e => setAmount(e.target.value)} 
              className="w-full border-2 border-emerald-200 focus:border-emerald-500 focus:ring-0 p-4 rounded-lg text-lg" 
              placeholder="請輸入存入金額" 
            />
          </div>
          
          <div className="flex gap-4 pt-4">
            <button type="button" onClick={() => navigate('/admin')} className="flex-1 py-3 text-gray-600 font-bold bg-gray-100 rounded-lg hover:bg-gray-200 transition">取消返回</button>
            <button type="submit" disabled={isProcessing} className="flex-[2] bg-emerald-600 text-white py-3 rounded-lg font-bold hover:bg-emerald-700 disabled:bg-emerald-400 transition">確認存入</button>
          </div>
        </form>
      </div>
    </div>
  );
}