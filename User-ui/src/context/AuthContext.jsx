import React, { createContext, useState, useEffect, useContext } from 'react';
import { jwtDecode } from 'jwt-decode';
import api from '../api/api';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initializeAuth = async () => {
      setLoading(true);
      try {
        const localToken = localStorage.getItem('token');
        if (localToken) {
          const decodedToken = jwtDecode(localToken);

          if (decodedToken.exp * 1000 > Date.now()) {
            try {
        
              const response = await api.get('/profile/me'); 
              setUser(response.data);
            } catch (profileError) {
              console.error("Failed to fetch user profile, logging out:", profileError);
              logout();
            }
          } else {
            logout();
          }
        } else {
          setUser(null);
        }
      } catch (error) {
        console.error("Invalid token or error during auth setup:", error);
        logout();
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, [token]);

  const login = (newToken) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  const authContextValue = {
    user,
    setUser,
    token,
    loading,
    login,
    logout,
    isAdmin: user && user.role === 'ROLE_ADMIN',
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