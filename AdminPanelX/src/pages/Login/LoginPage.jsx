import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { BsShieldLockFill } from 'react-icons/bs';
import { useMutation } from '@tanstack/react-query';
import { loginUser } from '../../services/AuthService';
import './LoginPage.css';

function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();

  // จัดการ logic การ login ด้วย mutation
  const loginMutation = useMutation({
    mutationFn: (credentials) => loginUser(credentials.email, credentials.password),
    onSuccess: async (token) => {
        // เมื่อ login สำเร็จ, เก็บ token และ redirect
        await login(token);
        navigate('/');
    },
    onError: (err) => {
        setError(err.message);
    }
  });

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    
    // เรียกใช้ mutation
    loginMutation.mutate({ email, password });
  };

  return (
    <div className="login-container">
      <Card className="login-card">
        <Card.Body>
          <div className="text-center mb-4">
            <BsShieldLockFill className="login-icon" />
            <h2 className="login-title mt-2">Admin Panel</h2>
          </div>
          
          {error && (
            <Alert variant="danger" className="login-alert">
              {error}
            </Alert>
          )}
          
          <Form onSubmit={handleLogin}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="login-input"
                disabled={loginMutation.isPending}
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
                disabled={loginMutation.isPending}
              />
            </Form.Group>

            <Button
              className="w-100 login-button"
              type="submit"
              variant="primary"
              disabled={loginMutation.isPending}
            >
              {loginMutation.isPending ? (
                <Spinner
                  as="span"
                  animation="border"
                  size="sm"
                  role="status"
                  aria-hidden="true"
                />
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