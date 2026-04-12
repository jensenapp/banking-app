import React, { useState } from 'react';
// useLoaderData: 用來接收 React Router 在進入頁面「前」預先載入的資料
// Link: React Router 的路由跳轉元件，取代傳統的 <a> 標籤以避免畫面重新整理 (SPA)
import { useLoaderData, Link } from 'react-router-dom';
// toast: 用來顯示成功或失敗的提示訊息 (彈出式小通知)
import { toast } from 'react-toastify';
// 匯入自定義的 API 服務，用來與後端進行溝通
import { getUserProfile } from '../services/UserService';
import { getMyAccounts, transfer } from '../services/AccountService';

export default function Dashboard() {
  // ==========================================
  // 1. 初始化資料 (來自 React Router Loader)
  // ==========================================
  // 解構取出由底下 dashboardLoader 預先抓取好的資料
  const { initialProfile, initialAccounts } = useLoaderData();
  
  // 將帳戶列表存入 React 的 state 中。
  // 為什麼不直接用 initialAccounts？因為轉帳成功後，我們需要重新拉取最新餘額並更新畫面，
  // 使用 state 才能透過 setAccounts 觸發 React 重新渲染畫面。
  const [accounts, setAccounts] = useState(initialAccounts);

  // ==========================================
  // 2. 轉帳視窗 (Modal) 的 State (狀態管理)
  // ==========================================
  // 控制彈出視窗是否顯示的開關 (true 顯示 / false 隱藏)
  const [isModalOpen, setIsModalOpen] = useState(false);
  // 紀錄使用者點擊了哪一個「轉出帳戶」的完整物件資訊
  const [selectedAccount, setSelectedAccount] = useState(null); 
  // 雙向綁定：紀錄使用者在輸入框打的「轉入帳戶 ID」
  const [targetId, setTargetId] = useState('');                 
  // 雙向綁定：紀錄使用者在輸入框打的「轉帳金額」
  const [amount, setAmount] = useState('');                     

  // ==========================================
  // 3. 核心邏輯 (Function)
  // ==========================================
  
  // 打開視窗並重置輸入框
  // 當使用者點擊特定帳戶卡片上的「轉帳」按鈕時會觸發此函式
  const openModal = (account) => {
    setSelectedAccount(account); // 把選中的帳戶存進 state
    setTargetId('');             // 清空上次可能留下來的目標帳號輸入紀錄
    setAmount('');               // 清空上次可能留下來的金額輸入紀錄
    setIsModalOpen(true);        // 打開 Modal 視窗
  };

  // 關閉視窗函式
  const closeModal = () => setIsModalOpen(false);

  // 執行轉帳 API (綁定在表單的 onSubmit 事件)
  const handleTransfer = async (e) => {
    e.preventDefault(); // 防止表單送出時瀏覽器預設的「重新整理畫面」行為
    
    try {
      // 呼叫後端轉帳 API
      await transfer({
        fromAccountId: selectedAccount.id, // 從我們剛剛存進 state 的選中帳戶拿 ID
        toAccountId: Number(targetId),     // 將輸入框的字串轉成數字格式送出
        amount: Number(amount)             // 將輸入框的字串轉成數字格式送出
      });
      
      toast.success("轉帳成功！"); // 顯示成功提示
      closeModal();                // 成功後自動關閉 Modal
      
      // 轉帳成功後，舊的餘額資料已經過時了。
      // 所以我們重新呼叫 API 取得最新帳戶狀態，並更新 state 讓畫面重新渲染。
      const res = await getMyAccounts();
      setAccounts(res.data);

    } catch (error) {
      // 如果 API 回傳錯誤，嘗試抓取後端自定義的錯誤訊息 (error.response.data.message)
      // 如果抓不到，就顯示預設的 "轉帳失敗" (使用可選串連 ?. 避免 undefined 報錯)
      toast.error(error.response?.data?.message || "轉帳失敗");
    }
  };

  // ==========================================
  // 4. 畫面渲染 (UI)
  // ==========================================
  return (
    <div className="p-8 max-w-5xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">帳戶總覽</h1>

      <div className="flex gap-8">
        
        {/* === 左邊：個人資料區塊 === */}
        {/* 這裡的資料直接使用 loader 預先抓取的 initialProfile，因為個人資料通常不會在這一頁頻繁變動 */}
        <div className="w-1/3 bg-white p-6 border rounded shadow">
          <h2 className="text-xl font-bold mb-4 border-b pb-2">個人資料</h2>
          <p><strong>帳號：</strong> {initialProfile.username}</p>
          <p><strong>信箱：</strong> {initialProfile.email}</p>
          <p><strong>權限：</strong> {initialProfile.roles.join(', ')}</p>
        </div>

        {/* === 右邊：帳戶列表區塊 === */}
        <div className="w-2/3">
          {/* 條件渲染：判斷如果 state 中的帳戶陣列為空，顯示提示訊息 */}
          {accounts.length === 0 ? (
            <div className="bg-gray-100 p-8 text-center rounded">
              您目前沒有帳戶，請攜帶證件至實體分行辦理。
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-4">
              {/* 用 map 迴圈把陣列裡的每一個帳戶資料轉換成 JSX 卡片元件 */}
              {accounts.map(acc => (
                <div key={acc.id} className="bg-blue-800 text-white p-6 rounded shadow">
                  <div className="flex justify-between mb-2">
                    <span>{acc.accountHolderName}</span>
                    <span className="text-sm border px-1 rounded">ID: {acc.id}</span>
                  </div>
                  <div className="text-2xl font-bold my-4">
                    {/* toLocaleString() 可以幫數字加上千分位逗號，例如 1000 變成 1,000 */}
                    $ {acc.balance.toLocaleString()}
                  </div>
                  
                  {/* 點擊時呼叫 openModal，並把當前的帳戶物件(acc)當作參數傳進去 */}
                  <button 
                    onClick={() => openModal(acc)}
                    className="w-full bg-white text-blue-800 py-2 rounded font-bold mb-2 hover:bg-gray-200"
                  >
                    轉帳
                  </button>
                  
                  {/* 使用 React Router 的 Link 進行動態路由導覽，帶上帳戶 ID */}
                  <Link to={`/accounts/${acc.id}/transactions`} className="text-sm underline text-blue-200 block text-center">
                    查看明細
                  </Link>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* === 彈出視窗 (Modal) === */}
      {/* 條件渲染：只有當 isModalOpen 為 true 的時候，這整塊 JSX 才會被渲染出來 */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
          <div className="bg-white p-6 rounded-lg w-96 shadow-xl">
            <h3 className="text-xl font-bold mb-4">跨帳戶轉帳</h3>
            
            {/* 表單送出時觸發 handleTransfer */}
            <form onSubmit={handleTransfer} className="space-y-4">
              {/* 顯示剛剛被選中的帳戶資訊 */}
              <div className="bg-gray-100 p-3 rounded">
                <p>轉出帳戶：ID {selectedAccount.id}</p>
                <p className="text-blue-600 font-bold">可用餘額：${selectedAccount.balance}</p>
              </div>

              <div>
                <label className="block mb-1 font-bold">轉入帳戶 ID</label>
                {/* 雙向綁定：value 對應 targetId state，onChange 負責把輸入的值寫回 state */}
                <input 
                  type="number" required className="border w-full p-2 rounded"
                  value={targetId} onChange={e => setTargetId(e.target.value)} 
                />
              </div>

              <div>
                <label className="block mb-1 font-bold">轉帳金額</label>
                {/* min="1" 確保 HTML 基礎驗證金額不能為 0 或負數 */}
                <input 
                  type="number" required min="1" className="border w-full p-2 rounded"
                  value={amount} onChange={e => setAmount(e.target.value)} 
                />
              </div>

              <div className="flex justify-end gap-2 mt-4">
                {/* type="button" 很重要，避免點擊取消時不小心觸發表單送出 */}
                <button type="button" onClick={closeModal} className="px-4 py-2 border rounded hover:bg-gray-100">取消</button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">確認轉帳</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// ==========================================
// 5. React Router 的 Loader
// ==========================================
// 這個 Loader 函式會在 React Router 準備切換到此元件「之前」執行。
// 好處是可以避免畫面先進來出現「載入中...」的閃爍，資料備齊了才渲染畫面。
export async function dashboardLoader() {
  try {
    // 使用 Promise.all 同時發起兩個 API 請求，節省等待時間
    const [profileRes, accountsRes] = await Promise.all([
      getUserProfile(),
      getMyAccounts()
    ]);
    
    // 將結果組合成一個物件回傳，元件內部就可以用 useLoaderData() 拿到
    return { initialProfile: profileRes.data, initialAccounts: accountsRes.data };
  } catch (error) {
    // 如果發生錯誤 (例如 Token 過期、沒權限等)，拋出一個 Response 交給 Router 的 ErrorBoundary 處理
    throw new Response("無法取得資料，請重新登入。", { status: error.response?.status || 500 });
  }
}