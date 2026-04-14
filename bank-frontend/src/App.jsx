// src/App.jsx
import { Routes, Route } from "react-router-dom";
import HeaderComponent from "./components/HeaderComponent";
import FooterComponent from "./components/FooterComponent";
import Home from './components/Home.jsx';
import Login from './components/Login.jsx';
import Signup from './components/Signup.jsx';
import Dashboard from './components/Dashboard.jsx';
import TransactionHistory from './components/TransactionHistory.jsx';
import AdminPanel from './components/AdminPanel.jsx';
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import ErrorPage from './components/ErrorPage.jsx';

import DepositPage from './components/DepositPage.jsx';
import WithdrawPage from './components/WithdrawPage.jsx';
import CreateAccountPage from './components/CreateAccountPage.jsx'; // 引入新頁面
import PublicRoute from "./components/PublicRoute.jsx";
import TransferPage from "./components/TransferPage.jsx";

function App() {
  return (
    <div className="flex flex-col min-h-screen bg-gray-100">
      <HeaderComponent />
      
      <main className="flex-grow flex flex-col relative">
        <Routes>
          <Route path="/" element={<Home />} />

         <Route element={<PublicRoute/>}> 
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          </Route>
          

          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/accounts/:id/transactions" element={<TransactionHistory />} />
            <Route path="/accounts/:id/transfer" element={<TransferPage/>}/>
          </Route>

          <Route element={<ProtectedRoute allowedRoles={["ROLE_ADMIN"]} />}>
            <Route path="/admin" element={<AdminPanel />} />
            <Route path="/admin/accounts/create" element={<CreateAccountPage />} />
            <Route path="/admin/accounts/:id/deposit" element={<DepositPage />} />
            <Route path="/admin/accounts/:id/withdraw" element={<WithdrawPage />} />
          </Route>

          <Route path="*" element={<ErrorPage />} />
        </Routes>
      </main>

      <FooterComponent />
    </div>
  );
}

export default App;