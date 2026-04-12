import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider, createRoutesFromElements, Route } from "react-router-dom";
import Login, { loginAction } from './components/Login.jsx';
import Signup, { signupAction } from './components/Signup.jsx';
import Dashboard, { dashboardLoader } from './components/Dashboard.jsx';

import './index.css';
import { ToastContainer, Bounce } from "react-toastify";
import "react-toastify/dist/ReactToastify.css"; 
import { AuthProvider } from './store/auth-context.jsx';
import Home from './components/Home.jsx';

import App from './App.jsx';
import ErrorPage from './components/ErrorPage.jsx';
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import TransactionHistory from './components/TransactionHistory.jsx';

import AdminRoute from './components/AdminRoute.jsx';
import AdminPanel from './components/AdminPanel.jsx';




// 定義路由變數
const routeDefinitions = createRoutesFromElements(
  <Route path="/" element={<App />} errorElement={<ErrorPage />}>
    
    {/* 公開路由 */}
    <Route index element={<Home />} />
    <Route path="/login" element={<Login />} action={loginAction} />
    <Route path="/signup" element={<Signup />} action={signupAction} /> 
    
    {/* 受保護的路由 (必須登入) */}
    <Route element={<ProtectedRoute />}>
    <Route path="/dashboard" element={<Dashboard />} loader={dashboardLoader} />
    <Route path="/accounts/:id/transactions" element={<TransactionHistory />} />
    </Route>

    <Route element={<AdminRoute />}>
      <Route path="/admin" element={<AdminPanel />} />
    </Route>

  </Route>
);

const appRouter = createBrowserRouter(routeDefinitions);

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthProvider>
      <RouterProvider router={appRouter} />
    </AuthProvider>
    
    {/* 全域的提示框設定 */}
    <ToastContainer
      position="top-center"
      autoClose={3000}
      hideProgressBar={false}
      newestOnTop={false}
      closeOnClick
      rtl={false}
      pauseOnFocusLoss
      draggable
      pauseOnHover
      theme="light"
      transition={Bounce}
    />
  </StrictMode>,
);