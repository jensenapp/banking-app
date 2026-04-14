// src/components/WithdrawPage.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAccount, withdraw } from '../services/AccountService';

export default function WithdrawPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [account, setAccount] = useState(null);
  const [amount, setAmount] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    const fetchAccountDetails = async () => {
      try {
        const response = await getAccount(id);
        setAccount(response.data);
      } catch (error) {
        toast.error("無法取得帳戶資訊");
        navigate('/admin');
      } finally {
        setIsLoading(false);
      }
    };
    fetchAccountDetails();
  }, [id, navigate]);

  const handleWithdraw = async (e) => {
    e.preventDefault();
    const numAmount = Number(amount);
    if (numAmount <= 0) return toast.warn("請輸入大於 0 的金額");

    setIsProcessing(true);
    try {
      await withdraw(id, { amount: numAmount });
      toast.success(`提款成功！已提取 $${numAmount.toLocaleString()}`);
      navigate('/admin');
    } catch (error) {
      toast.error(error.response?.data?.message || "提款失敗，請確認餘額或稍後再試。");
      setIsProcessing(false);
    }
  };

  if (isLoading) return <div className="text-center py-20 text-xl font-bold">載入中...</div>;
  if (!account) return null;

  return (
    <div className="max-w-2xl mx-auto px-4 py-10 w-full">
      <div className="bg-white rounded-xl shadow-lg overflow-hidden border border-gray-200">
        <div className="bg-amber-600 px-6 py-4">
          <h2 className="text-xl font-bold text-white">臨櫃提款作業</h2>
        </div>
        
        <form onSubmit={handleWithdraw} className="p-8 space-y-6">
          <div className="bg-gray-50 p-5 rounded-lg border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-500 mb-2">扣款帳戶資訊</h3>
            <div className="flex justify-between items-end">
              <p className="text-lg text-gray-900 font-bold">ID: {account.id} <span className="font-medium text-gray-600">({account.accountHolderName})</span></p>
              <p className="text-amber-600 font-bold text-lg">可用餘額: ${account.balance.toLocaleString()}</p>
            </div>
          </div>
          
          <div>
            <label className="block text-base font-semibold text-gray-700 mb-2">提款金額 (TWD)</label>
            <input 
              type="number" required min="1" max={account.balance}
              value={amount} onChange={e => setAmount(e.target.value)} 
              className="w-full border-2 border-amber-200 focus:border-amber-500 focus:ring-0 p-4 rounded-lg text-lg" 
              placeholder="請輸入提款金額" 
            />
          </div>
          
          <div className="flex gap-4 pt-4">
            <button type="button" onClick={() => navigate('/admin')} className="flex-1 py-3 text-gray-600 font-bold bg-gray-100 rounded-lg hover:bg-gray-200 transition">取消返回</button>
            <button type="submit" disabled={isProcessing} className="flex-[2] bg-amber-600 text-white py-3 rounded-lg font-bold hover:bg-amber-700 disabled:bg-amber-400 transition">確認提取</button>
          </div>
        </form>
      </div>
    </div>
  );
}