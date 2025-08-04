import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Card, Form, Button, Alert, Spinner } from 'react-bootstrap'; // Add Spinner
import { BsShieldLockFill } from 'react-icons/bs';

import { loginUser } from '../../services/AuthService';

import './LoginPage.css';

function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoggingIn, setIsLoggingIn] = useState(false); // Add loading state for the button
  const { login } = useAuth();
  const navigate = useNavigate();

  // ===================================================================
  // ===== THIS IS THE OTHER CRITICAL FIX ==============================
  // ===================================================================
  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoggingIn(true); // Start loading

    try {
      const token = await loginUser(email, password);
      await login(token); 
      
      navigate('/'); 

    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoggingIn(false);
    }
  };

  return (
    <div className="login-container">
      <Card className="login-card">
        <Card.Body>
          <div className="text-center mb-4">
            <BsShieldLockFill className="login-icon" />
            <h2 className="login-title mt-2">Admin Panel</h2>
          </div>
          
          {error && <Alert variant="danger" className="login-alert">{error}</Alert>}
          
          <Form onSubmit={handleLogin}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="login-input"
                disabled={isLoggingIn}
              />
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="login-input"
                disabled={isLoggingIn}
              />
            </Form.Group>
            <Button className="w-100 login-button" type="submit" variant="primary" disabled={isLoggingIn}>
              {isLoggingIn ? (
                <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />
              ) : (
                'Log In'
              )}
            </Button>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
}

export default LoginPage;