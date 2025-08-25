import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';

import { useAuth } from './context/AuthContext';

import AdminLayout from './layouts/AdminLayout/AdminLayout';
import LoginPage from './pages/Login/LoginPage';
import Dashboard from './pages/Dashboard/Dashboard';
import ComponentsPage from './pages/Components/ComponentsPage';
import AddComponentPage from './pages/AddComponent/AddComponentPage';
import EditComponentPage from './pages/EditComponent/EditComponentPage';
import LookupsPage from './pages/Lookups/LookupsPage';
import OrdersPage from './pages/OrdersPage/OrdersPage';
import OrderDetailPage from './pages/OrderDetailPage/OrderDetailPage';
import ShippingProvidersPage from './pages/ShippingProvidersPage/ShippingProvidersPage';
import UsersPage from './pages/UsersPage/UsersPage';

import AccessDeniedRedirect from './components/AccessDeniedRedirect/AccessDeniedRedirect';

import './styles/common.css'; 
import './styles/ImagePreview.css';

// สร้าง component PrivateRoute ขึ้นมาเพื่อจัดการการเข้าถึงหน้าต่างๆ ของ admin
const PrivateRoute = () => {
    const { user, isAdmin, isLoading } = useAuth(); 

    if (isLoading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: 'var(--primary-bg)' }}>
            </div>
        );
    }

    if (!user) { 
        return <Navigate to="/login" replace />; 
    }

    if (!isAdmin) {
        return <AccessDeniedRedirect />;
    }

    // ถ้าผ่านเงื่อนไขทั้งหมด ก็ให้แสดง AdminLayout ซึ่งจะมี <Outlet> สำหรับ render หน้าลูกๆ ต่อไป
    return <AdminLayout />;
};

const AppRouter = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      
      {/* Route หลัก "/" จะใช้ PrivateRoute เป็นตัวคุม และ Route ลูกทั้งหมดจะถูก render ภายใน AdminLayout */}
      <Route path="/" element={<PrivateRoute />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="components" element={<ComponentsPage />} />
        <Route path="add-component" element={<AddComponentPage />} />
        <Route path="edit-component/:id" element={<EditComponentPage />} />
        <Route path="lookups" element={<LookupsPage />} /> 
        <Route path="orders" element={<OrdersPage />} />
        <Route path="order-details/:orderId" element={<OrderDetailPage />} /> 
        <Route path="shipping-providers" element={<ShippingProvidersPage />} /> 
        <Route path="users" element={<UsersPage />} /> 
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};


export default AppRouter;