

import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axiosConfig';
import './Auth.css'; 

const API_BASE_URL = import.meta.env.VITE_BACKEND_URL;

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const { login } = useAuth();

  
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const response = await api.post('/login', { email, password });
            const token = response.data.jwt || response.data.token;
            if (!token) {
                throw new Error("Login successful but no token was returned.");
            }
            login(token);
            navigate('/');
        } catch (err) {
            if (err.response && (err.response.status === 401 || err.response.status === 403)) {
                setError('อีเมลหรือรหัสผ่านไม่ถูกต้อง');
            } else {
                setError('การเข้าสู่ระบบผิดพลาด กรุณาลองใหม่อีกครั้ง');
            }
            console.error('Login error:', err);
        }
    };

    

    return (
        <div className="auth-popup">
            <div className="auth-header">
                <button className="close-btn" onClick={() => navigate('/')}>×</button>
            </div>
            <form onSubmit={handleSubmit} className="auth-form">
                <h2>เข้าสู่ระบบ</h2>
                {error && <p className="error-message">{error}</p>}
                
                {}
                <div className="input-group">
                    <input type="email" placeholder="@อีเมล" value={email} onChange={(e) => setEmail(e.target.value)} required />
                </div>
                <div className="input-group">
                    <input type="password" placeholder="รหัสผ่าน" value={password} onChange={(e) => setPassword(e.target.value)} required />
                </div>
                <div className="form-options">
                    <label>
                        <input type="checkbox"/>
                        จดจำเมื่อเข้าสู่ระบบ
                    </label>
                </div>
                <button type="submit" className="submit-btn">
                    เข้าสู่ระบบ
                </button>

                {}
                <div className="divider">
                    <span>หรือ</span>
                </div>
                <div className="social-login">
                    {}
                    <a 
                        href={`${API_BASE_URL}/oauth2/authorization/google`} 
                        className="google-login-btn"
                    >
                        <img src="https://developers.google.com/identity/images/g-logo.png" alt="Google logo" />
                        <span>เข้าสู่ระบบด้วย Google</span>
                    </a>
                </div>
                
                <div className="switch-form-text">
                    <p>
                        ไม่ใช่สมาชิก?{' '}
                        <Link to="/register">สมัครสมาชิก</Link>
                    </p>
                </div>

                <div className="forgot-password">
                    <Link to="/forgot-password">ลืมรหัสผ่าน ?</Link>
                </div>
            </form>
        </div>
    );
};

export default LoginPage;