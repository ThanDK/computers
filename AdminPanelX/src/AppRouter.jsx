// src/AppRouter.jsx  OR  src/router/AppRouter.js

import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
// --- THIS IS THE FIX ---
// The path is changed from "../context/AuthContext" to "./context/AuthContext"
import { useAuth } from './context/AuthContext';
// ----------------------

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
import './styles/common.css'; 
import './styles/ImagePreview.css';
const PrivateRoute = () => {
    const { user, isAdmin, isLoading } = useAuth(); // Get the new isLoading state

    // 1. First, check if we are still loading the auth state
    if (isLoading) {
        // You can render a loading spinner here for a better user experience
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: 'var(--primary-bg)' }}>
                {/* Optional: Add a Bootstrap Spinner or any loading component */}
            </div>
        );
    }

    // 2. After loading is finished, check for the user
    if (!user) { 
        return <Navigate to="/login" replace />; 
    }

    // 3. Finally, check for admin role
    if (!isAdmin) {
        return (
            <div style={{ textAlign: 'center', marginTop: '5rem', color: 'white' }}>
                <h1>Access Denied</h1>
                <p>You do not have permission to view this page.</p>
            </div>
        );
    }

    // If all checks pass, render the layout
    return <AdminLayout />;
};

const AppRouter = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      
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
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRouter;