import React, { createContext, useState, useEffect, useContext } from 'react';
import { jwtDecode } from 'jwt-decode';

// Create the context
export const AuthContext = createContext(null);

// Create the provider component
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token')); // Use function for initial read
  const [isLoading, setIsLoading] = useState(true); // --- 1. ADD IS_LOADING STATE ---

  useEffect(() => {
    if (token) {
      try {
        const decodedToken = jwtDecode(token);

        if (decodedToken.exp * 1000 > Date.now()) {
          // IMPORTANT FIX: Also set the user ID
          setUser({ 
            id: decodedToken.userId, 
            email: decodedToken.sub, 
            roles: decodedToken.roles || [] 
          });
        } else {
          console.warn("AuthContext: Token has expired.");
          logout();
        }
      } catch (error) {
        console.error("AuthContext: Invalid token.", error);
        logout();
      } finally {
        setIsLoading(false); // --- 2. SET LOADING TO FALSE AFTER CHECKING ---
      }
    } else {
      setIsLoading(false); // --- 2. SET LOADING TO FALSE IF NO TOKEN ---
    }
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
    token,
    isLoading, // --- 3. EXPOSE IS_LOADING IN CONTEXT VALUE ---
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

// Custom hook to use the auth context
export const useAuth = () => {
  return useContext(AuthContext);
};