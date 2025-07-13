import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

import AdminLayout from './layouts/AdminLayout/AdminLayout';
import LoginPage from './pages/Login/LoginPage';
import Dashboard from './pages/Dashboard/Dashboard';
import ComponentsPage from './pages/Components/ComponentsPage';
import AddComponentPage from './pages/AddComponent/AddComponentPage';
import EditComponentPage from './pages/EditComponent/EditComponentPage';
import LookupsPage from './pages/Lookups/LookupsPage'; // <-- IMPORT NEW PAGE

// ... PrivateRoute component is unchanged ...
const PrivateRoute = () => {
    const { user, isAdmin } = useAuth();
    if (!user) { return <Navigate to="/login" replace />; }
    if (!isAdmin) {
        return (
            <div style={{ textAlign: 'center', marginTop: '5rem', color: 'white' }}>
                <h1>Access Denied</h1>
                <p>You do not have permission to view this page.</p>
            </div>
        );
    }
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
        <Route path="lookups" element={<LookupsPage />} /> {/* <-- REPLACE 'contacts' WITH 'lookups' */}
        {/* Remove other unused routes like /invoices, /calendar etc. if you wish */}
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRouter;