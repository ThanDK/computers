import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const AccessDeniedRedirect = () => {
    const [countdown, setCountdown] = useState(3);
    const navigate = useNavigate();
    const { logout } = useAuth();

    // useEffect ตัวนี้จัดการเรื่องนับถอยหลังทั้งหมด
    useEffect(() => {
        // พอเวลานับถอยหลังหมด ก็ให้ logout แล้วเด้งไปหน้า login
        if (countdown <= 0) {
            logout();
            navigate('/login', { replace: true });
            return; 
        }

        const timerId = setTimeout(() => {
            setCountdown(countdown - 1);
        }, 1000);

        // cleanup function: เผื่อ user กดออกจากหน้านี้เอง timer จะได้ไม่ทำงานมั่ว
        return () => clearTimeout(timerId);

    }, [countdown, logout, navigate]); 

    return (
        <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            justifyContent: 'center', 
            alignItems: 'center', 
            height: '100vh', 
            backgroundColor: 'var(--primary-bg)', 
            color: 'var(--text-primary)',
            textAlign: 'center',
            padding: '1rem'
        }}>
            <h1>Access Denied</h1>
            <p>You do not have permission to access this area.</p>
            <p style={{ marginTop: '1rem', color: 'var(--text-secondary)' }}>
                Redirecting to login in {countdown} seconds...
            </p>
        </div>
    );
};

export default AccessDeniedRedirect;