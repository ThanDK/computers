import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../../components/Sidebar/Sidebar';
import ScreenSizeWarning from '../../components/ScreenSizeWarning/ScreenSizeWarning';
import { Toaster } from 'react-hot-toast';
import './AdminLayout.css';

// Layout หลักของหน้า Admin ทั้งหมด
const AdminLayout = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const toggleSidebar = () => setIsCollapsed(!isCollapsed);

  return (
    <>
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: 'var(--secondary-bg)',
            color: 'var(--text-primary)',
            border: '1px solid #4a5a76',
          },
          success: {
            iconTheme: {
              primary: '#22c55e',
              secondary: 'var(--primary-bg)',
            },
          },
          error: {
            iconTheme: {
              primary: '#ef4444',
              secondary: 'var(--primary-bg)',
            },
          },
        }}
      />

      <ScreenSizeWarning />

      <div className={`app-container ${isCollapsed ? 'sidebar-collapsed' : ''}`}>
        <Sidebar
          isCollapsed={isCollapsed}
          toggleSidebar={toggleSidebar}
        />
        
        <main className="content-wrapper">
          <Outlet />
        </main>
      </div>
    </>
  );
};

export default AdminLayout;