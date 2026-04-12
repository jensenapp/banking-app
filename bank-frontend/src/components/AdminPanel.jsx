import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify'; // 用於顯示系統提示訊息（成功、錯誤等）
// 引入與後端 API 溝通的 Service 函式
import { getAllAccounts, deleteAccount, deposit, withdraw, createAccount } from '../services/AccountService';

export default function AdminPanel() {
  // ==========================================
  // 全域與資料列表狀態 (Global & List State)
  // ==========================================
  
  // accounts: 儲存目前頁面要顯示的帳戶資料陣列
  const [accounts, setAccounts] = useState([]);
  
  // pageInfo: 控制分頁的狀態物件
  // - pageNo: 目前頁碼 (0-indexed，0 代表第一頁)
  // - totalPages: 總頁數
  // - last: 布林值，判斷是否為最後一頁
  const [pageInfo, setPageInfo] = useState({ pageNo: 0, totalPages: 1, last: true });
  
  // isLoading: 控制資料載入中畫面顯示的狀態
  const [isLoading, setIsLoading] = useState(true);

  // ==========================================
  // 模態框 (Modal) 狀態：存提款作業
  // ==========================================
  
  // 控制存提款 Modal 的開關
  const [isTxModalOpen, setIsTxModalOpen] = useState(false);
  // 紀錄目前被選中準備進行存提款操作的帳戶物件
  const [selectedAccount, setSelectedAccount] = useState(null);
  // 交易類型：'DEPOSIT' (存款) 或 'WITHDRAW' (提款)
  const [txType, setTxType] = useState('DEPOSIT');
  // 交易金額，使用字串以避免 input 的 initial value 問題，後續會轉型為數字
  const [txAmount, setTxAmount] = useState('');
  // 判斷 API 是否正在處理中，用於禁用按鈕防止重複送出
  const [isProcessing, setIsProcessing] = useState(false);

  // ==========================================
  // 模態框 (Modal) 狀態：開戶作業
  // ==========================================
  
  // 控制開戶 Modal 的開關
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  // 新開戶綁定的目標使用者 ID
  const [targetUserId, setTargetUserId] = useState('');
  // 開戶時的初始存入餘額
  const [initialBalance, setInitialBalance] = useState('');

  // ==========================================
  // 生命週期與資料載入邏輯
  // ==========================================
  
  /**
   * 當 pageInfo.pageNo 改變時（也就是使用者切換頁面時），觸發資料重新載入。
   * 初次渲染時也會觸發一次（因為 pageNo 初始化為 0）。
   */
  useEffect(() => {
    fetchAllAccounts(pageInfo.pageNo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageInfo.pageNo]);

  /**
   * 根據頁碼向後端請求帳戶列表資料
   * @param {number} page - 要抓取的頁碼
   */
  const fetchAllAccounts = async (page) => {
    setIsLoading(true); // 開啟載入中狀態
    try {
      // 呼叫 API，預設每頁顯示 5 筆 (可依需求調整)
      const response = await getAllAccounts(page, 5);
      setAccounts(response.data.content); // 更新帳戶列表資料
      // 更新分頁資訊以供 UI 使用
      setPageInfo({
        pageNo: response.data.pageNo,
        totalPages: response.data.totalPages,
        last: response.data.last
      });
    } catch (error) {
      toast.error("無法載入帳戶資料，請確認網路連線。");
    } finally {
      setIsLoading(false); // 無論成功失敗皆關閉載入中狀態
    }
  };

  /**
   * 處理刪除帳戶的操作
   * @param {number} id - 要刪除的帳戶 ID
   * @param {string} holderName - 帳戶持有人名稱（用於確認對話框提示）
   */
  const handleDelete = async (id, holderName) => {
    // 再次向使用者確認是否要刪除，避免誤觸
    if (!window.confirm(`警告：您確定要強制刪除 [${holderName}] 的帳戶 (ID: ${id}) 嗎？此動作無法復原！`)) return;
    try {
      await deleteAccount(id); // 呼叫刪除 API
      toast.success(`帳戶 ID: ${id} 已成功刪除`);
      fetchAllAccounts(pageInfo.pageNo); // 刪除成功後刷新當前頁面資料
    } catch (error) {
      // 若後端有回傳錯誤訊息則顯示，否則顯示預設訊息（例如帳戶仍有關聯交易無法刪除）
      toast.error(error.response?.data?.message || "刪除失敗，該帳戶可能還有關聯交易紀錄。");
    }
  };

  // ==========================================
  // 臨櫃存提款邏輯
  // ==========================================
  
  /**
   * 處理存提款表單送出事件
   */
  const handleTransactionSubmit = async (e) => {
    e.preventDefault(); // 阻止表單預設的重整行為
    const numAmount = Number(txAmount); // 將字串輸入轉為數字
    
    // 基本的防呆檢驗
    if (numAmount <= 0) return toast.warn("請輸入大於 0 的金額");

    setIsProcessing(true); // 防連點機制
    try {
      if (txType === 'DEPOSIT') {
        // 執行存款 API
        await deposit(selectedAccount.id, { amount: numAmount });
        toast.success(`臨櫃存款成功！已存入 $${numAmount.toLocaleString()}`);
      } else {
        // 執行提款 API
        await withdraw(selectedAccount.id, { amount: numAmount });
        toast.success(`臨櫃提款成功！已提取 $${numAmount.toLocaleString()}`);
      }
      setIsTxModalOpen(false); // 成功後關閉 Modal
      fetchAllAccounts(pageInfo.pageNo); // 刷新列表顯示最新餘額
    } catch (error) {
      // 處理提款餘額不足或系統錯誤等狀況
      toast.error(error.response?.data?.message || "交易失敗，請確認餘額或稍後再試。");
    } finally {
      setIsProcessing(false); // 關閉防連點機制
    }
  };

  // ==========================================
  // 替客戶開戶邏輯
  // ==========================================
  
  /**
   * 處理建立新帳戶表單送出事件
   */
  const handleCreateSubmit = async (e) => {
    e.preventDefault(); // 阻止表單預設行為
    const balance = Number(initialBalance); // 轉為數字
    const userId = Number(targetUserId);

    // 基本防呆檢驗
    if (!userId) return toast.warn("請輸入目標客戶 ID");
    if (balance < 0) return toast.warn("初始餘額不能為負數");

    setIsProcessing(true);
    try {
      // 執行開戶 API，傳送 UserID 與初始金額
      await createAccount({ userId: userId, balance: balance });
      toast.success(`成功為客戶 ID: ${userId} 開立新帳戶！`);
      setIsCreateModalOpen(false); // 開戶成功後關閉 Modal
      fetchAllAccounts(pageInfo.pageNo); // 刷新列表以顯示新帳戶
    } catch (error) {
      toast.error(error.response?.data?.message || "開戶失敗，請檢查客戶 ID 是否正確，或是否已達開戶上限。");
    } finally {
      setIsProcessing(false);
    }
  };

  // ==========================================
  // 畫面渲染區 (JSX)
  // ==========================================
  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10 w-full">
      
      {/* 區塊：頁首標題與建立按鈕 */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end mb-8 border-b pb-4 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-900">管理員控制台 (Teller System)</h1>
          <p className="text-gray-500 mt-2">檢視全行帳戶，並執行臨櫃開戶、存款與提款作業</p>
        </div>
        <button 
          onClick={() => { 
            // 開啟開戶 Modal 前，先清空舊的表單狀態
            setTargetUserId(''); 
            setInitialBalance(''); 
            setIsCreateModalOpen(true); 
          }}
          className="bg-indigo-600 text-white font-bold px-5 py-2 rounded-lg hover:bg-indigo-700 transition shadow-sm"
        >
          + 替客戶開立帳戶
        </button>
      </div>

      {/* 區塊：帳戶總表 */}
      <div className="bg-white rounded-xl shadow-md overflow-hidden border border-gray-200">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            {/* 表格標題列 */}
            <thead className="bg-gray-800">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase">帳戶 ID</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase">持有人名稱</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-gray-300 uppercase">目前餘額 (TWD)</th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-300 uppercase">管理操作</th>
              </tr>
            </thead>
            
            {/* 表格內容列 */}
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                // 狀態：資料載入中
                <tr><td colSpan="4" className="text-center py-10 text-gray-500 font-medium">資料載入中...</td></tr>
              ) : accounts.length === 0 ? (
                // 狀態：資料為空
                <tr><td colSpan="4" className="text-center py-10 text-gray-500">系統中尚無任何帳戶</td></tr>
              ) : (
                // 狀態：正常渲染帳戶列表
                accounts.map((acc) => (
                  <tr key={acc.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-500">#{acc.id}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900">{acc.accountHolderName}</td>
                    {/* 使用 toLocaleString() 將數字加上千位逗號 */}
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-right text-blue-600">$ {acc.balance.toLocaleString()}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-center text-sm font-medium space-x-2">
                      {/* 操作按鈕：存提款 */}
                      <button
                        onClick={() => { 
                          setSelectedAccount(acc); // 設定目標帳戶
                          setTxAmount('');         // 清空輸入框
                          setTxType('DEPOSIT');    // 預設為存款模式
                          setIsTxModalOpen(true);  // 開啟 Modal
                        }}
                        className="text-green-700 bg-green-50 hover:bg-green-100 px-3 py-1.5 rounded transition border border-green-200"
                      >
                        臨櫃存提款
                      </button>
                      {/* 操作按鈕：刪除 */}
                      <button
                        onClick={() => handleDelete(acc.id, acc.accountHolderName)}
                        className="text-red-700 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded transition border border-red-200"
                      >
                        刪除
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* 區塊：分頁控制項 */}
        {/* 只有在非載入中且有資料時才顯示分頁 */}
        {!isLoading && accounts.length > 0 && (
          <div className="bg-gray-50 px-6 py-4 border-t border-gray-200 flex items-center justify-between">
            {/* 上一頁按鈕：若在第一頁 (pageNo === 0) 則禁用 */}
            <button 
              disabled={pageInfo.pageNo === 0} 
              onClick={() => setPageInfo(prev => ({ ...prev, pageNo: prev.pageNo - 1 }))} 
              className="text-sm bg-white border border-gray-300 font-medium px-4 py-2 rounded-md hover:bg-gray-100 disabled:opacity-50 transition"
            >
              上一頁
            </button>
            <span className="text-sm font-medium text-gray-600">第 {pageInfo.pageNo + 1} 頁 / 共 {pageInfo.totalPages} 頁</span>
            {/* 下一頁按鈕：若已是最後一頁 (pageInfo.last) 則禁用 */}
            <button 
              disabled={pageInfo.last} 
              onClick={() => setPageInfo(prev => ({ ...prev, pageNo: prev.pageNo + 1 }))} 
              className="text-sm bg-white border border-gray-300 font-medium px-4 py-2 rounded-md hover:bg-gray-100 disabled:opacity-50 transition"
            >
              下一頁
            </button>
          </div>
        )}
      </div>

      {/* ========================================== */}
      {/* 區塊：存提款 Modal (彈出視窗) */}
      {/* ========================================== */}
      {/* 只有在 isTxModalOpen 為 true 且已經選定帳戶時才渲染 */}
      {isTxModalOpen && selectedAccount && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            {/* Modal 標題列 */}
            <div className="bg-green-700 px-6 py-4 flex justify-between">
              <h3 className="text-lg font-bold text-white">臨櫃存提款作業</h3>
              <button onClick={() => setIsTxModalOpen(false)} className="text-white">✕</button>
            </div>
            
            <form onSubmit={handleTransactionSubmit} className="p-6 space-y-4">
              {/* 顯示被操作帳戶的基本資訊 */}
              <div className="bg-gray-50 p-4 rounded-lg border border-gray-100">
                <p className="text-sm text-gray-500">操作帳戶</p>
                <div className="flex justify-between items-end mt-1">
                  <p className="text-gray-900 font-medium">ID: {selectedAccount.id} ({selectedAccount.accountHolderName})</p>
                  <p className="text-blue-600 font-bold">餘額: ${selectedAccount.balance.toLocaleString()}</p>
                </div>
              </div>
              
              {/* 存提款類型切換 Tabs */}
              <div className="flex p-1 space-x-1 bg-gray-200 rounded-xl">
                <button type="button" onClick={() => setTxType('DEPOSIT')} className={`flex-1 py-2 text-sm font-semibold rounded-lg ${txType === 'DEPOSIT' ? 'bg-white text-green-700 shadow' : 'text-gray-500'}`}>存 款</button>
                <button type="button" onClick={() => setTxType('WITHDRAW')} className={`flex-1 py-2 text-sm font-semibold rounded-lg ${txType === 'WITHDRAW' ? 'bg-white text-red-600 shadow' : 'text-gray-500'}`}>提 款</button>
              </div>
              
              {/* 金額輸入框 */}
              <input type="number" required min="1" value={txAmount} onChange={e => setTxAmount(e.target.value)} className="w-full border p-3 rounded-lg" placeholder="輸入金額 (TWD)" />
              
              {/* 提交按鈕：在處理中時會禁用 */}
              <button type="submit" disabled={isProcessing} className="w-full bg-green-600 text-white py-3 rounded-lg font-medium hover:bg-green-700 disabled:bg-green-400">確認執行</button>
            </form>
          </div>
        </div>
      )}

      {/* ========================================== */}
      {/* 區塊：替客戶開戶 Modal (彈出視窗) */}
      {/* ========================================== */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            {/* Modal 標題列 */}
            <div className="bg-indigo-600 px-6 py-4 flex justify-between">
              <h3 className="text-lg font-bold text-white">替客戶開立新帳戶</h3>
              <button onClick={() => setIsCreateModalOpen(false)} className="text-white">✕</button>
            </div>
            
            <form onSubmit={handleCreateSubmit} className="p-6 space-y-4">
              {/* 使用者 ID 輸入框 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">目標客戶 User ID</label>
                <input type="number" required min="1" value={targetUserId} onChange={e => setTargetUserId(e.target.value)} className="w-full border p-3 rounded-lg bg-gray-50" placeholder="請輸入客戶的使用者 ID (例如: 1)" />
              </div>
              
              {/* 初始餘額輸入框 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">初始存入餘額 (TWD)</label>
                <input type="number" required min="0" value={initialBalance} onChange={e => setInitialBalance(e.target.value)} className="w-full border p-3 rounded-lg bg-gray-50" placeholder="0" />
              </div>
              
              {/* 取消與確認按鈕群組 */}
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setIsCreateModalOpen(false)} className="flex-1 border py-2.5 rounded-lg text-gray-600 hover:bg-gray-50">取消</button>
                <button type="submit" disabled={isProcessing} className="flex-1 bg-indigo-600 text-white py-2.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50">確認開戶</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}