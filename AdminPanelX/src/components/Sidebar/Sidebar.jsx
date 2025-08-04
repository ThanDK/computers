import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  BsGrid1X2Fill,
  BsFillGrid3X3GapFill,
  BsWrench,
  BsListCheck,
  BsPeopleFill,
  BsJustify,
  BsTruck,
  BsPersonCircle
} from 'react-icons/bs';
import './Sidebar.css';

function Sidebar({ isCollapsed, toggleSidebar }) {
  const { user, isAdmin } = useAuth();

  if (!isAdmin || !user) {
    return null; 
  }

  return (
    <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}>
      <div className="sidebar-header">
        <div className="sidebar-profile-header">
          {user.profilePictureUrl ? (
            <img src={user.profilePictureUrl} alt="Admin" className="sidebar-avatar" />
          ) : (
            <BsPersonCircle className="sidebar-avatar" /> 
          )}
          
          <div className="sidebar-user-info">
            <div className="sidebar-user-name-container">
              <span className="sidebar-status-dot"></span>
              <span className="sidebar-user-name" title={user.name}>{user.name}</span>
            </div>
            <span className="sidebar-user-role-badge">{user.role.replace('ROLE_', '')}</span>
          </div>
        </div>

        <button className="sidebar-toggle-btn" onClick={toggleSidebar}>
          <BsJustify />
        </button>
      </div>

      <nav className="sidebar-nav">
        <div className="sidebar-divider" />
        <div className="sidebar-category">General</div>
        <NavLink to="/dashboard" className="sidebar-link" end>
          <BsGrid1X2Fill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Dashboard</span>
        </NavLink>


        <div className="sidebar-divider" />
        <div className="sidebar-category">Component Management</div>
        <NavLink to="/components" className="sidebar-link">
          <BsFillGrid3X3GapFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Components</span>
        </NavLink>
        <NavLink to="/lookups" className="sidebar-link">
          <BsWrench className='sidebar-link-icon' />
          <span className="sidebar-link-text">Lookups</span>
        </NavLink>
        
        <div className="sidebar-divider" />
        <div className="sidebar-category">Order Management</div>
        <NavLink to="/orders" className="sidebar-link">
          <BsListCheck className='sidebar-link-icon' />
          <span className="sidebar-link-text">Orders</span>
        </NavLink>
        <NavLink to="/shipping-providers" className="sidebar-link">
          <BsTruck className='sidebar-link-icon' />
          <span className="sidebar-link-text">Shipping Providers</span>
        </NavLink>

        <div className="sidebar-divider" />
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