// src/components/TransferPage.jsx
import React, {useState, useEffect, useRef} from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAccount, transfer } from '../services/AccountService';

export default function TransferPage() {
  const { id } = useParams(); // 從網址取得轉出帳戶的 ID
  const navigate = useNavigate();
  
  const [account, setAccount] = useState(null);
  const [targetId, setTargetId] = useState('');
  const [amount, setAmount] = useState('');
  
  const [isLoading, setIsLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

    const idempotencyKeyRef = useRef(crypto.randomUUID());

  // 進入頁面時，抓取轉出帳戶的資訊，確認餘額與身份
  useEffect(() => {
    const fetchAccountDetails = async () => {
      try {
        const response = await getAccount(id);
        setAccount(response.data);
      } catch (error) {
        toast.error("無法取得帳戶資訊，請返回儀表板重新操作");
        navigate('/dashboard');
      } finally {
        setIsLoading(false);
      }
    };
    fetchAccountDetails();
  }, [id, navigate]);

  const handleTransferSubmit = async (e) => {
    e.preventDefault();
    const numAmount = Number(amount);
    const numTargetId = Number(targetId);

    if (numAmount <= 0) return toast.warn("轉帳金額必須大於 0");
    if (!numTargetId) return toast.warn("請輸入有效的轉入帳戶 ID");
    if (numTargetId === Number(id)) return toast.warn("無法轉帳給自己");

    setIsProcessing(true);
    try {
      await transfer({
        fromAccountId: id,
        toAccountId: numTargetId,
        amount: numAmount,
          idempotencyKey:idempotencyKeyRef.current
      });
      
      toast.success(`成功轉帳 $${numAmount.toLocaleString()} 至帳戶 #${numTargetId}！`);
      navigate('/dashboard'); // 轉帳成功後導回儀表板
    } catch (error) {
      toast.error(error.response?.data?.message || "轉帳失敗，請確認餘額或帳戶狀態");
      setIsProcessing(false);
    }
  };

  if (isLoading) return <div className="text-center py-20 text-xl font-bold text-blue-800">載入中...</div>;
  if (!account) return null;

  return (
    <div className="max-w-2xl mx-auto px-4 py-10 w-full">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">跨帳戶轉帳</h1>
        <Link to="/dashboard" className="text-blue-600 hover:underline font-medium">
          ← 返回儀表板
        </Link>
      </div>

      <div className="bg-white rounded-xl shadow-lg overflow-hidden border border-gray-200">
        <div className="bg-blue-600 px-6 py-4">
          <h2 className="text-xl font-bold text-white">輸入轉帳資訊</h2>
        </div>
        
        <form onSubmit={handleTransferSubmit} className="p-8 space-y-6">
          {/* 轉出帳戶資訊顯示 */}
          <div className="bg-blue-50 p-5 rounded-lg border border-blue-100">
            <h3 className="text-sm font-semibold text-blue-800 mb-2">轉出帳戶 (From)</h3>
            <div className="flex justify-between items-end">
              <p className="text-lg text-gray-900 font-bold">ID: {account.id} <span className="font-medium text-gray-600">({account.accountHolderName})</span></p>
              <p className="text-blue-700 font-bold text-lg">可用餘額: ${account.balance.toLocaleString()}</p>
            </div>
          </div>
          
          {/* 目標帳戶輸入 */}
          <div>
            <label className="block text-base font-semibold text-gray-700 mb-2">轉入帳戶 ID (To)</label>
            <input 
              type="number" required min="1" 
              value={targetId} onChange={e => setTargetId(e.target.value)} 
              className="w-full border-2 border-gray-200 focus:border-blue-500 focus:ring-0 p-4 rounded-lg text-lg bg-gray-50" 
              placeholder="請輸入收款人的帳戶 ID" 
            />
          </div>

          {/* 金額輸入 */}
          <div>
            <label className="block text-base font-semibold text-gray-700 mb-2">轉帳金額 (TWD)</label>
            <input 
              type="number" required min="1" max={account.balance}
              value={amount} onChange={e => setAmount(e.target.value)} 
              className="w-full border-2 border-gray-200 focus:border-blue-500 focus:ring-0 p-4 rounded-lg text-lg bg-gray-50" 
              placeholder="請輸入轉帳金額" 
            />
          </div>
          
          {/* 按鈕區 */}
          <div className="flex gap-4 pt-4">
            <button type="button" onClick={() => navigate('/dashboard')} className="flex-1 py-3 text-gray-600 font-bold bg-gray-100 rounded-lg hover:bg-gray-200 transition">取消</button>
            <button type="submit" disabled={isProcessing} className="flex-[2] bg-blue-600 text-white py-3 rounded-lg font-bold hover:bg-blue-700 disabled:bg-blue-400 transition">確認轉出</button>
          </div>
        </form>
      </div>
    </div>
  );
}