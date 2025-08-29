import React, { createContext, useState, useEffect, useContext, useCallback } from 'react';
import { jwtDecode } from 'jwt-decode';
import { fetchCurrentUserProfile } from '../services/ProfileService';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  // อ่าน token เริ่มต้นจาก localStorage
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  // state loading, active ตอนเช็ค token เริ่มต้นเมื่อเปิดเว็บ
  const [isLoading, setIsLoading] = useState(true);

  // โหลดข้อมูล user, ใช้ useCallback เพื่อไม่ให้ฟังก์ชันนี้ถูกสร้างใหม่ทุกครั้งที่ re-render
  const loadUserContext = useCallback(async (currentToken) => {
    if (!currentToken) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      // ถอดรหัส token เพื่อเช็ควันหมดอายุ
      const decodedToken = jwtDecode(currentToken);
      if (decodedToken.exp * 1000 < Date.now()) {
        throw new Error("Token expired.");
      }
      
      // ถ้า token ยังใช้ได้, ดึงข้อมูล user profile เต็มๆ จาก API
      const fullUserProfile = await fetchCurrentUserProfile(currentToken);
      setUser(fullUserProfile);

    } catch (error) {
      console.error("AuthContext: Failed to load user.", error.message);
      // ถ้ามีปัญหา (เช่น token หมดอายุ), ให้เคลียร์ข้อมูลทั้งหมดทิ้ง (logout)
      localStorage.removeItem('token');
      setToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Effect นี้จะทำงานครั้งแรกตอนเปิดเว็บ และทุกครั้งที่ token เปลี่ยน
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
  
  // อัปเดตข้อมูล user ใน context ตรงๆ (เช่น หลังแก้โปรไฟล์) ไม่ต้อง fetch ใหม่
  const updateCurrentUser = (updatedUserData) => {
    setUser(prevUser => ({ ...prevUser, ...updatedUserData }));
  };

  // ค่าทั้งหมดที่จะส่งไปให้ components ลูกผ่าน context
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

// Custom hook ให้ component อื่นเรียกใช้ง่ายๆ
export const useAuth = () => {
  return useContext(AuthContext);
};