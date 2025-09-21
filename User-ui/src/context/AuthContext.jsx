import React, { createContext, useState, useEffect, useContext } from 'react';
import { jwtDecode } from 'jwt-decode';
import axios from 'axios';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    try {
      if (token) {
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        const decodedToken = jwtDecode(token);

        if (decodedToken.exp * 1000 > Date.now()) {
          let roles = decodedToken.roles || [];
          if (typeof roles === 'string') {
            roles = roles.split(/[\s,]+/);
          }
          
          setUser({ email: decodedToken.sub, roles: roles });
        } else {
          logout();
        }
      } else {
        setUser(null);
        delete axios.defaults.headers.common['Authorization'];
      }
    } catch (error) {
      console.error("Invalid token or error during auth setup:", error);
      logout();
    } finally {
      setLoading(false);
    }
  }, [token]);

  const login = (newToken) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
  };

  const authContextValue = {
    user,
    setUser, 
    token,
    loading,
    login,
    logout,
    isAdmin: user && Array.isArray(user.roles) && user.roles.includes('ROLE_ADMIN'),
  };

  return (
    <AuthContext.Provider value={authContextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};