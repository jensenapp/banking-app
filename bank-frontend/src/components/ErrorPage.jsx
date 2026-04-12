import React from "react";
import { useRouteError, Link } from "react-router-dom";
import HeaderComponent from "./HeaderComponent";
import FooterComponent from "./FooterComponent";

export default function ErrorPage() {
  const routeError = useRouteError();

  let errorTitle = "Oops! Something went wrong";
  let errorMessage = "An unexpected error occurred. Please try again later.";

  if (routeError) {
    errorTitle = routeError.status || "Error";
    errorMessage = routeError.data?.message || routeError.data || routeError.message || errorMessage;
  }

  return (
    <div className="flex flex-col min-h-screen bg-gray-50">
      <HeaderComponent />
      <main className="flex-grow flex flex-col items-center justify-center py-10">
        <div className="text-center p-8 bg-white rounded-lg shadow-md max-w-md w-full">
          <h1 className="text-4xl font-extrabold text-red-600 mb-4">{errorTitle}</h1>
          <p className="text-lg text-gray-700 font-medium mb-6">{errorMessage}</p>
          <Link
            to="/"
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition"
          >
            返回首頁
          </Link>
        </div>
      </main>
      <FooterComponent />
    </div>
  );
}