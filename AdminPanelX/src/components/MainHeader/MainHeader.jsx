import React from 'react';
import { BsBoxArrowRight } from 'react-icons/bs';
import { Button } from 'react-bootstrap';
import { useAuth } from '../../context/AuthContext';
import './MainHeader.css';

function MainHeader() {
  const { logout } = useAuth();

  return (
    <header className='main-header'>
      <div className='header-actions'>
        <Button variant="outline-danger" className="logout-btn" onClick={logout}>
          <BsBoxArrowRight className="me-2" />
          Logout
        </Button>
      </div>
    </header>
  );
}

export default MainHeader;