import React from 'react'
import { useAuth } from '../store/auth-context'
import { Navigate, Outlet } from 'react-router-dom';

export default function PublicRoute() {

    const {isAuthenticated,roles}=useAuth();

    if(isAuthenticated){
     const targetPath=roles.includes("ROLE_ADMIN") ? "/admin" : "/dashboard";
     return <Navigate to={targetPath} replace/>
    }
    
    return <Outlet/>;
}


