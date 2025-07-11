import React, { createContext, useState, useEffect, useContext } from 'react';
import { jwtDecode } from 'jwt-decode';

// Create the context
export const AuthContext = createContext(null);

// Create the provider component
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));

  useEffect(() => {
    if (token) {
      try {
        const decodedToken = jwtDecode(token);
        // Check if the token is expired
        if (decodedToken.exp * 1000 > Date.now()) {
          // Token is valid, set user state
          setUser({ email: decodedToken.sub, roles: decodedToken.roles || [] }); // Added safety || []
        } else {
          // Token is expired
          console.warn("AuthContext: Token has expired.");
          logout();
        }
      } catch (error) {
        // Token is malformed or invalid
        console.error("AuthContext: Invalid token.", error);
        logout();
      }
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

  // The value passed to consumers. `isAdmin` is calculated on the fly, which is a great pattern.
  const authContextValue = {
    user,
    token,
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