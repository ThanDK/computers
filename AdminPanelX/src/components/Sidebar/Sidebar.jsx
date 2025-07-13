import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  BsGrid1X2Fill,
  BsFillGrid3X3GapFill,
  BsListCheck,
  BsFillCalendarEventFill,
  BsQuestionCircleFill,
  BsBarChartFill,
  BsPieChartFill,
  BsGeoAltFill,
  BsJustify,
  BsWrench // MODIFICATION: Import a more suitable icon for Lookups
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
        <NavLink to="/" className="sidebar-link">
          <BsGrid1X2Fill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Dashboard</span>
        </NavLink>

        <div className="sidebar-category">Data</div>
        <NavLink to="/components" className="sidebar-link">
          <BsFillGrid3X3GapFill className='sidebar-link-icon' />
          <span className="sidebar-link-text">Components</span>
        </NavLink>
        
        {/* --- THIS IS THE ONLY MODIFIED SECTION --- */}
        <NavLink to="/lookups" className="sidebar-link">
          <BsWrench className='sidebar-link-icon' />
          <span className="sidebar-link-text">Lookups</span>
        </NavLink>
        {/* --- END OF MODIFICATION --- */}
        
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