import React from 'react';
import './TableControls.css'; 


const TableControls = ({ children }) => {
    return (
        <div className="table-controls-wrapper">
            {children}
        </div>
    );
};

export default TableControls;