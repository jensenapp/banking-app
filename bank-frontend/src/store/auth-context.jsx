import { createContext, useContext, useReducer, useEffect } from "react";

export const AuthContext = createContext();
export const useAuth = () => useContext(AuthContext);

const LOGIN_SUCCESS = "LOGIN_SUCCESS";
const LOGOUT = "LOGOUT";

const authReducer = (prevState, action) => {
  switch (action.type) {
    case LOGIN_SUCCESS:
      return {
        ...prevState,
        jwtToken: action.payload.jwtToken,
        username: action.payload.username, 
        roles: action.payload.roles,       // 存入角色陣列 (ex: ["ROLE_USER"])
        isAuthenticated: true,
      };
    case LOGOUT:
      return {
        jwtToken: null,
        username: null,
        roles: [],
        isAuthenticated: false,
      };
    default:
      return prevState;
  }
};

export const AuthProvider = ({ children }) => {
  const initialAuthState = (() => {
    try {
      const jwtToken = localStorage.getItem("jwtToken");
      const username = localStorage.getItem("username");
      const roles = localStorage.getItem("roles");
      
      if (jwtToken && username) {
        return {
          jwtToken,
          username,
          roles: roles ? JSON.parse(roles) : [],
          isAuthenticated: true,
        };
      }
    } catch (error) {
      console.error("Error loading auth state", error);
    }
    return { jwtToken: null, username: null, roles: [], isAuthenticated: false };
  })();

  const [authState, dispatch] = useReducer(authReducer, initialAuthState);

  useEffect(() => {
    if (authState.isAuthenticated) {
      localStorage.setItem("jwtToken", authState.jwtToken);
      localStorage.setItem("username", authState.username);
      localStorage.setItem("roles", JSON.stringify(authState.roles));
    } else {
      localStorage.removeItem("jwtToken");
      localStorage.removeItem("username");
      localStorage.removeItem("roles");
    }
  }, [authState]);

  const loginSuccess = (jwtToken, username, roles) => {
    dispatch({ type: LOGIN_SUCCESS, payload: { jwtToken, username, roles } });
  };

  const logout = () => {
    dispatch({ type: LOGOUT });
  };

  return (
    <AuthContext.Provider value={{ ...authState, loginSuccess, logout }}>
      {children}
    </AuthContext.Provider>
  );
};