import HeaderComponent from "./components/HeaderComponent";
import FooterComponent from "./components/FooterComponent";
import { Outlet, useNavigation } from "react-router-dom";

function App() {
  // 獲取 react-router 的導航狀態，用來判斷是否正在執行 loader 或 action
  const navigation = useNavigation();
  const isLoading = navigation.state === "loading" || navigation.state === "submitting";

  return (
    <div className="flex flex-col min-h-screen bg-gray-100">
      <HeaderComponent />
      
      {/* 畫面主體區域 */}
      <main className="flex-grow flex flex-col relative">
        {isLoading ? (
          <div className="absolute inset-0 z-40 flex items-center justify-center bg-gray-100 bg-opacity-75">
            <span className="text-2xl font-semibold text-blue-600 animate-pulse">
              處理中...
            </span>
          </div>
        ) : null}
        
        {/* 子路由對應的組件會渲染在這裡 */}
        <Outlet />
      </main>

      <FooterComponent />
    </div>
  );
}

export default App;