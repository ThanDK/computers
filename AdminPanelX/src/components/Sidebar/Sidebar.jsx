import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  BsGrid1X2Fill,
  BsFillGrid3X3GapFill,
  BsFillPersonLinesFill,
  BsListCheck,
  BsFillCalendarEventFill,
  BsQuestionCircleFill,
  BsBarChartFill,
  BsPieChartFill,
  BsGeoAltFill,
  BsJustify
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
        <NavLink to="/" className="sidebar-link">
          <BsGrid1X2Fill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Dashboard</span>
        </NavLink>

        <div className="sidebar-category">Data</div>
        <NavLink to="/components" className="sidebar-link">
          <BsFillGrid3X3GapFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Components</span>
        </NavLink>
        <NavLink to="/contacts" className="sidebar-link">
          <BsFillPersonLinesFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Contacts</span>
        </NavLink>
        <NavLink to="/invoices" className="sidebar-link">
          <BsListCheck className='sidebar-link-icon' />
          <span className="sidebar-link-text">Invoices</span>
        </NavLink>

        <div className="sidebar-category">Pages</div>
        <NavLink to="/calendar" className="sidebar-link">
          <BsFillCalendarEventFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Calendar</span>
        </NavLink>
        <NavLink to="/faq" className="sidebar-link">
          <BsQuestionCircleFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">FAQ Page</span>
        </NavLink>

        <div className="sidebar-category">Charts</div>
        <NavLink to="/bar" className="sidebar-link">
          <BsBarChartFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Bar Chart</span>
        </NavLink>
        <NavLink to="/pie" className="sidebar-link">
          <BsPieChartFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Pie Chart</span>
        </NavLink>
        <NavLink to="/geo" className="sidebar-link">
          <BsGeoAltFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Geography</span>
        </NavLink>
      </nav>
    </aside>
  );
}

export default Sidebar;