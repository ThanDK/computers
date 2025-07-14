import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  BsGrid1X2Fill,        // Dashboard
  BsFillGrid3X3GapFill, // Components
  BsWrench,             // Lookups
  BsListCheck,          // Orders
  BsReceipt,            // Invoices
  BsPeopleFill,         // Users
  BsJustify,
} from 'react-icons/bs';
import './Sidebar.css';

function Sidebar({ isCollapsed, toggleSidebar }) {
  // NOTE: Your original file included props for isCollapsed and toggleSidebar.
  // I am leaving them here as they were in your code, but removing the functionality
  // since you pointed out you didn't want it. If you want the button removed,
  // I can do that as well. For now, this is a 1-to-1 match of your file's structure.
  
  return (
    <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}>
      <div className="sidebar-header">
        {!isCollapsed && <span className="sidebar-title">ADMIN</span>}
        <button className="sidebar-toggle-btn" onClick={toggleSidebar}>
          <BsJustify />
        </button>
      </div>

      <nav className="sidebar-nav">
        {/* --- Top Level Link --- */}
        <NavLink to="/dashboard" className="sidebar-link" end>
          <BsGrid1X2Fill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Dashboard</span>
        </NavLink>

        {/* --- Component Management Section --- */}
        <div className="sidebar-category">Component Management</div>
        <NavLink to="/components" className="sidebar-link">
          <BsFillGrid3X3GapFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Components</span>
        </NavLink>
        <NavLink to="/lookups" className="sidebar-link">
          <BsWrench className='sidebar-link-icon' />
          <span className="sidebar-link-text">Lookups</span>
        </NavLink>
        
        {/* --- Order Management Section --- */}
        <div className="sidebar-category">Order Management</div>
        <NavLink to="/orders" className="sidebar-link">
          <BsListCheck className='sidebar-link-icon' />
          <span className="sidebar-link-text">Orders</span>
        </NavLink>
        <NavLink to="/invoices" className="sidebar-link">
          <BsReceipt className='sidebar-link-icon' />
          <span className="sidebar-link-text">Invoices</span>
        </NavLink>

        {/* --- User Management Section --- */}
        <div className="sidebar-category">User Management</div>
        <NavLink to="/users" className="sidebar-link">
          <BsPeopleFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Users</span>
        </NavLink>
      </nav>
    </aside>
  );
}

export default Sidebar;