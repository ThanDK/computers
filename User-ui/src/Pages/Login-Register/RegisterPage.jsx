import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import api from '../../api/axiosConfig'; 
import './Auth.css';

const RegisterPage = () => {
    
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (password !== confirmPassword) {
            setError('รหัสผ่านและการยืนยันรหัสผ่านไม่ตรงกัน');
            return;
        }

        try {
            
            await api.post('/register', { 
                name,
                email,
                password,
            });
            
            setSuccess('การสมัครสมาชิกสำเร็จ! กำลังนำคุณไปยังหน้าเข้าสู่ระบบ...');
            setTimeout(() => {
                navigate('/login');
            }, 2000);

        } catch (err) {
            
            if (err.response && err.response.data && err.response.data.message) {
                setError(err.response.data.message);
            } else if (err.response && (err.response.status === 409 || err.response.status === 400)) {
                setError('อีเมลนี้ถูกใช้งานแล้ว หรือข้อมูลไม่ถูกต้อง');
            } else {
                
                setError('การสมัครสมาชิกผิดพลาด กรุณาลองใหม่อีกครั้ง');
                console.error('Registration error details:', err); 
            }
        }
    };

   
    return (
        <div className="auth-popup">
            {}
            <form onSubmit={handleSubmit} className="auth-form">
                <h2>สมัครสมาชิก</h2>
                
                {error && <p className="error-message">{error}</p>}
                {success && <p className="success-message">{success}</p>}

                <div className="input-group">
                    <input
                        type="text"
                        placeholder="ชื่อ" 
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                    />
                </div>

                <div className="input-group">
                    <input
                        type="email"
                        placeholder="อีเมล" 
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>

                <div className="input-group">
                    <input
                        type="password"
                        placeholder="รหัสผ่าน"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>

                <div className="input-group">
                    <input
                        type="password"
                        placeholder="ยืนยันรหัสผ่าน"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                    />
                </div>

                <button type="submit" className="submit-btn">
                    สมัครสมาชิก
                </button>
            </form>
        </div>
    );
};

export default RegisterPage;