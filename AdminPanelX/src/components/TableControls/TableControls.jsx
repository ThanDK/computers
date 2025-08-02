import React from 'react';
import './TableControls.css'; // Import its own dedicated CSS file

/**
 * A reusable container for table action/filter bars. It provides the consistent
 * background box and flexbox layout for all pages.
 * @param {object} props
 * @param {React.ReactNode} props.children - The content to render inside (e.g., filters, buttons).
 */
const TableControls = ({ children }) => {
    return (
        <div className="table-controls-wrapper">
            {children}
        </div>
    );
};

export default TableControls;