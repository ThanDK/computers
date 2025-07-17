// src/components/Sidebar/Sidebar.js
import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  BsGrid1X2Fill,
  BsFillGrid3X3GapFill,
  BsWrench,
  BsListCheck,
  BsReceipt,
  BsPeopleFill,
  BsJustify,
  BsTruck
} from 'react-icons/bs';
import './Sidebar.css';

function Sidebar({ isCollapsed, toggleSidebar }) {
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
        <NavLink to="/shipping-providers" className="sidebar-link">
          <BsTruck className='sidebar-link-icon' />
          <span className="sidebar-link-text">Shipping Providers</span>
        </NavLink>
        <NavLink to="/reports" className="sidebar-link">
          <BsReceipt className='sidebar-link-icon' />
          <span className="sidebar-link-text">Reports</span>
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