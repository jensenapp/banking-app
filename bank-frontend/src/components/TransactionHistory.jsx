import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getTransactions } from '../services/AccountService';
import { toast } from 'react-toastify';

export default function TransactionHistory() {
  const { id } = useParams(); // 從 URL 取得 accountId
  const [transactions, setTransactions] = useState([]);
  const [pageInfo, setPageInfo] = useState({ pageNo: 0, totalPages: 1, last: true });
  const [isLoading, setIsLoading] = useState(true);

  // 每次 pageNo 改變時，自動發送 API 請求抓資料
  useEffect(() => {
    fetchTransactions(pageInfo.pageNo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageInfo.pageNo, id]);

  const fetchTransactions = async (page) => {
    setIsLoading(true);
    try {
      const response = await getTransactions(id, page, 5); // 預設一頁 5 筆
      setTransactions(response.data.content);
      setPageInfo({
        pageNo: response.data.pageNo,
        totalPages: response.data.totalPages,
        last: response.data.last
      });
    } catch (error) {
      toast.error("無法載入交易紀錄");
    } finally {
      setIsLoading(false);
    }
  };

  const formatType = (type) => {
    switch (type) {
      case 'DEPOSIT': return <span className="text-blue-600 font-bold">存款</span>;
      case 'WITHDRAW': return <span className="text-red-600 font-bold">提款</span>;
      case 'TRANSFER_IN': return <span className="text-green-600 font-bold">轉入</span>;
      case 'TRANSFER_OUT': return <span className="text-orange-600 font-bold">轉出</span>;
      default: return type;
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-10 w-full">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">交易明細 (帳戶 ID: {id})</h1>
        <Link to="/dashboard" className="text-blue-600 hover:underline font-medium">
          ← 返回總覽
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase">交易時間</th>
              <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase">交易類型</th>
              <th className="px-6 py-4 text-right text-xs font-semibold text-gray-500 uppercase">金額 (TWD)</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {isLoading ? (
              <tr><td colSpan="3" className="text-center py-10 text-gray-500">載入中...</td></tr>
            ) : transactions.length === 0 ? (
              <tr><td colSpan="3" className="text-center py-10 text-gray-500">尚無任何交易紀錄</td></tr>
            ) : (
              transactions.map((txn) => (
                <tr key={txn.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {new Date(txn.timestamp).toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {formatType(txn.transactionType)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-right text-gray-900">
                    $ {txn.amount.toLocaleString()}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* 分頁控制區 */}
        {!isLoading && transactions.length > 0 && (
          <div className="bg-gray-50 px-6 py-3 border-t border-gray-200 flex items-center justify-between">
            <button 
              disabled={pageInfo.pageNo === 0}
              onClick={() => setPageInfo(prev => ({ ...prev, pageNo: prev.pageNo - 1 }))}
              className="text-sm bg-white border border-gray-300 px-4 py-2 rounded-md hover:bg-gray-100 disabled:opacity-50"
            >
              上一頁
            </button>

            <span className="text-sm text-gray-600">
              第 {pageInfo.pageNo + 1} 頁 / 共 {pageInfo.totalPages} 頁
            </span>
            
            <button 
              disabled={pageInfo.last}
              onClick={() => setPageInfo(prev => ({ ...prev, pageNo: prev.pageNo + 1 }))}
              className="text-sm bg-white border border-gray-300 px-4 py-2 rounded-md hover:bg-gray-100 disabled:opacity-50"
            >
              下一頁
            </button>
          </div>
        )}
      </div>
    </div>
  );
}