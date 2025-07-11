import React from 'react';
import './PageHeader.css';

function PageHeader({ title, subtitle }) {
  return (
    <div className='page-header'>
      <h2>{title}</h2>
      <h5>{subtitle}</h5>
    </div>
  );
}

export default PageHeader;