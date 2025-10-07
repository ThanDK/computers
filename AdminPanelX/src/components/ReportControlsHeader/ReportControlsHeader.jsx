import React from 'react';
import './ReportControlsHeader.css'; 

const ReportControlsHeader = ({ children }) => {
    return (
        <div className="report-controls-header-wrapper">
            {children}
        </div>
    );
};

export default ReportControlsHeader;