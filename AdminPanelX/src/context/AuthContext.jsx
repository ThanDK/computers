import React, { createContext, useState, useEffect, useContext, useCallback } from 'react';
import { jwtDecode } from 'jwt-decode';
import { fetchCurrentUserProfile } from '../services/ProfileService';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  // state ของ token จะอ่านค่าเริ่มต้นจาก localStorage
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  // isLoading จะเป็น true ตอนที่กำลังตรวจสอบ token และดึงข้อมูล user
  const [isLoading, setIsLoading] = useState(true);

  // สร้างฟังก์ชันสำหรับโหลดข้อมูล user context โดยใช้ useCallback เพื่อ performance
  const loadUserContext = useCallback(async (currentToken) => {
    if (!currentToken) {
      setUser(null);
      setIsLoading(false);
      return;
    }
    try {
      // ถอดรหัส token เพื่อเช็ควันหมดอายุ
      const decodedToken = jwtDecode(currentToken);
      if (decodedToken.exp * 1000 < Date.now()) throw new Error("Token expired.");
      // ถ้า token ยังใช้ได้ ก็ไปดึงข้อมูล user แบบเต็มๆ จาก API
      const fullUserProfile = await fetchCurrentUserProfile(currentToken);
      setUser(fullUserProfile);
    } catch (error) {
      console.error("AuthContext: Failed to load user.", error.message);
      // ถ้ามีปัญหา (เช่น token หมดอายุ) ก็จะเคลียร์ทุกอย่างทิ้ง
      localStorage.removeItem('token');
      setToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // effect นี้จะทำงานตอน component โหลดครั้งแรก หรือตอนที่ token มีการเปลี่ยนแปลง
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
  
  // ฟังก์ชันนี้มีไว้สำหรับอัปเดตข้อมูล user ใน context โดยตรง (เช่น หลังแก้โปรไฟล์)
  // โดยไม่ต้องไปโหลดใหม่ทั้งหมดจาก server
  const updateCurrentUser = (updatedUserData) => {
    setUser(prevUser => ({ ...prevUser, ...updatedUserData }));
  };

  // รวบรวมค่าทั้งหมดที่จะส่งไปให้ component ลูกผ่าน context
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

// custom hook สำหรับให้ component อื่นๆ เรียกใช้ค่าจาก AuthContext ได้ง่ายๆ
export const useAuth = () => {
  return useContext(AuthContext);
};