import React, { createContext, useState, useEffect, useContext, useCallback } from 'react';
import { jwtDecode } from 'jwt-decode';
import { fetchCurrentUserProfile } from '../services/ProfileService';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [isLoading, setIsLoading] = useState(true);

  const loadUserContext = useCallback(async (currentToken) => {
    if (!currentToken) {
      setUser(null);
      setIsLoading(false);
      return;
    }
    try {
      const decodedToken = jwtDecode(currentToken);
      if (decodedToken.exp * 1000 < Date.now()) throw new Error("Token expired.");
      const fullUserProfile = await fetchCurrentUserProfile(currentToken);
      setUser(fullUserProfile);
    } catch (error) {
      console.error("AuthContext: Failed to load user.", error.message);
      localStorage.removeItem('token');
      setToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUserContext(token);
  }, [token, loadUserContext]);


  const login = async (newToken) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    await loadUserContext(newToken);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };
  
  const updateCurrentUser = (updatedUserData) => {
    setUser(prevUser => ({ ...prevUser, ...updatedUserData }));
  };

  const authContextValue = {
    user,
    token,
    isLoading,
    login,
    logout,
    updateCurrentUser,
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