import { useAuth } from "../store/auth-context";
import { Navigate, Outlet} from "react-router-dom";
import { toast } from 'react-toastify';

export default function ProtectedRoute({allowedRoles}) {
  const { isAuthenticated,roles } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if(allowedRoles && allowedRoles.length>0){
  const hasPermission=allowedRoles.some(role=>roles.includes(role));
  if(!hasPermission){
    toast.error("權限不足，已為您導回儀表板");
     return <Navigate to="/dashboard"/>
  }
  }

  

  return <Outlet />;
}