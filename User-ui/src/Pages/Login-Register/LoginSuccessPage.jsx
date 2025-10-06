
import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const LoginSuccessPage = () => {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    
    const [message, setMessage] = useState('กำลังตรวจสอบข้อมูลและเข้าสู่ระบบ...');

    useEffect(() => {
        const token = searchParams.get('token');

        if (token) {
            
            login(token);
            
            
            setMessage('เข้าสู่ระบบสำเร็จ! กำลังนำคุณไปยังหน้าหลัก...');

           
            const timer = setTimeout(() => {
                navigate('/', { replace: true });
            }, 1500);

            
            return () => clearTimeout(timer);

        } else {
            console.error("Login success page reached without a token.");
            setMessage('เกิดข้อผิดพลาด: ไม่พบข้อมูลการเข้าสู่ระบบ');
            
            setTimeout(() => {
                navigate('/login', { replace: true });
            }, 3000);
        }
    }, [login, navigate, searchParams]); 

    return (
        <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            justifyContent: 'center', 
            alignItems: 'center', 
            height: '100vh',
            fontFamily: 'sans-serif'
        }}>
            {}
            <h2>{message}</h2>
            {}
        </div>
    );
};

export default LoginSuccessPage;