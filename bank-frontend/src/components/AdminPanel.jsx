import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { Link } from 'react-router-dom';
import { getAllAccounts, deleteAccount } from '../services/AccountService'; // 移除了 deposit, withdraw, createAccount

export default function AdminPanel() {
  const [accounts, setAccounts] = useState([]);
  const [pageInfo, setPageInfo] = useState({ pageNo: 0, totalPages: 1, last: true });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchAllAccounts(pageInfo.pageNo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageInfo.pageNo]);

  const fetchAllAccounts = async (page) => {
    setIsLoading(true);
    try {
      const response = await getAllAccounts(page, 5);
      setAccounts(response.data.content);
      setPageInfo({
        pageNo: response.data.pageNo,
        totalPages: response.data.totalPages,
        last: response.data.last
      });
    } catch (error) {
      toast.error("無法載入帳戶資料，請確認網路連線。");
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id, holderName) => {
    if (!window.confirm(`警告：您確定要強制刪除 [${holderName}] 的帳戶 (ID: ${id}) 嗎？此動作無法復原！`)) return;
    try {
      await deleteAccount(id);
      toast.success(`帳戶 ID: ${id} 已成功刪除`);
      fetchAllAccounts(pageInfo.pageNo); // 刪除成功後刷新列表
    } catch (error) {
      toast.error(error.response?.data?.message || "刪除失敗，該帳戶可能還有關聯交易紀錄。");
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10 w-full">
      
      {/* 標題區 */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end mb-8 border-b pb-4 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-900">管理員控制台</h1>
          <p className="text-gray-500 mt-2">檢視全行帳戶，並執行臨櫃開戶、存款與提款作業</p>
        </div>
        {/* 改為使用 Link 導向開戶頁面 */}
        <Link 
          to="/admin/accounts/create"
          className="bg-indigo-600 text-white font-bold px-5 py-2.5 rounded-lg hover:bg-indigo-700 transition shadow-sm inline-block"
        >
          + 替客戶開立帳戶
        </Link>
      </div>

      {/* 帳戶總表 */}
      <div className="bg-white rounded-xl shadow-md overflow-hidden border border-gray-200">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-800">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase">帳戶 ID</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-300 uppercase">持有人名稱</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-gray-300 uppercase">目前餘額 (TWD)</th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-300 uppercase">管理操作</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                <tr><td colSpan="4" className="text-center py-10 text-gray-500 font-medium">資料載入中...</td></tr>
              ) : accounts.length === 0 ? (
                <tr><td colSpan="4" className="text-center py-10 text-gray-500">系統中尚無任何帳戶</td></tr>
              ) : (
                accounts.map((acc) => (
                  <tr key={acc.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-500">#{acc.id}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900">{acc.accountHolderName}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-right text-blue-600">$ {acc.balance.toLocaleString()}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-center text-sm font-medium space-x-2">
                      
                      <Link
                        to={`/admin/accounts/${acc.id}/deposit`}
                        className="inline-block text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-3 py-1.5 rounded transition border border-emerald-200 font-semibold"
                      >
                        存款
                      </Link>
                      
                      <Link
                        to={`/admin/accounts/${acc.id}/withdraw`}
                        className="inline-block text-amber-700 bg-amber-50 hover:bg-amber-100 px-3 py-1.5 rounded transition border border-amber-200 font-semibold"
                      >
                        提款
                      </Link>

                      <button
                        onClick={() => handleDelete(acc.id, acc.accountHolderName)}
                        className="inline-block text-red-700 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded transition border border-red-200"
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

        {/* 分頁 */}
        {!isLoading && accounts.length > 0 && (
          <div className="bg-gray-50 px-6 py-4 border-t border-gray-200 flex items-center justify-between">
            <button 
              disabled={pageInfo.pageNo === 0} 
              onClick={() => setPageInfo(prev => ({ ...prev, pageNo: prev.pageNo - 1 }))} 
              className="text-sm bg-white border border-gray-300 font-medium px-4 py-2 rounded-md hover:bg-gray-100 disabled:opacity-50 transition"
            >
              上一頁
            </button>
            <span className="text-sm font-medium text-gray-600">第 {pageInfo.pageNo + 1} 頁 / 共 {pageInfo.totalPages} 頁</span>
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
    </div>
  );
}