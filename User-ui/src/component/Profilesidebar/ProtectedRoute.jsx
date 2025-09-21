

import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Spinner } from 'react-bootstrap';

const ProtectedRoute = ({ children }) => {
    const { token, loading } = useAuth();

    
    if (loading) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ height: '80vh' }}>
                <Spinner animation="border" />
            </div>
        );
    }

    if (!token) {
        
        return <Navigate to="/login" replace />;
    }

    
    return children;
};

export default ProtectedRoute;