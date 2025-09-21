import React, { useState, useEffect } from 'react';
import { Card, Form, Button, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { FaUserCircle, FaSave, FaTimes } from 'react-icons/fa';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api';

const ProfileEdit = () => {
    const { user, setUser } = useAuth(); 
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        name: '',
        email: '',
        password: '',
        confirmPassword: ''
    });
    const [profilePicture, setProfilePicture] = useState(null);
    const [preview, setPreview] = useState(null);

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    
    useEffect(() => {
        const fetchUserProfile = async () => {
            if (!user) {
                navigate('/login');
                return;
            }
            try {
                setLoading(true);
                const response = await api.get('/profile/me');
                setFormData({
                    name: response.data.name,
                    email: response.data.email,
                    password: '',
                    confirmPassword: ''
                });
                setPreview(response.data.profilePictureUrl);
            } catch (err) {
                setError("ไม่สามารถโหลดข้อมูลผู้ใช้ได้");
            } finally {
                setLoading(false);
            }
        };
        fetchUserProfile();
    }, [user, navigate]);


    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

 
    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setProfilePicture(file);
            setPreview(URL.createObjectURL(file));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        if (formData.password && formData.password !== formData.confirmPassword) {
            setError("รหัสผ่านใหม่และการยืนยันรหัสผ่านไม่ตรงกัน");
            return;
        }

        setSubmitting(true);

        try {
         
            const submissionData = new FormData();

            
            const profileData = {
                name: formData.name,
                email: formData.email,
            };
            
            if (formData.password) {
                profileData.password = formData.password;
            }

            
            submissionData.append('profileData', new Blob([JSON.stringify(profileData)], { type: 'application/json' }));
            
            if (profilePicture) {
                submissionData.append('file', profilePicture);
            }

            const response = await api.put('/profile', submissionData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

          
            setUser(response.data);

            setSuccess("บันทึกข้อมูลส่วนตัวสำเร็จ!");
            setTimeout(() => navigate('/profile'), 1500); 

        } catch (err) {
            console.error("Failed to update profile:", err);
            const errorMessage = err.response?.data?.message || "เกิดข้อผิดพลาดในการบันทึกข้อมูล";
            setError(errorMessage);
        } finally {
            setSubmitting(false);
        }
    };


    if (loading) {
        return <Spinner animation="border" />;
    }

    return (
        <Card className="p-4">
            <Card.Body>
                <h4 className="mb-4">แก้ไขข้อมูลส่วนตัว</h4>
                
                {error && <Alert variant="danger">{error}</Alert>}
                {success && <Alert variant="success">{success}</Alert>}

                <Form onSubmit={handleSubmit}>
                    <Row className="align-items-center mb-4">
                        <Col xs="auto">
                            {preview ? (
                                <img src={preview} alt="Profile Preview" className="rounded-circle" style={{ width: '80px', height: '80px', objectFit: 'cover' }} />
                            ) : (
                                <FaUserCircle size={80} className="text-secondary" />
                            )}
                        </Col>
                        <Col>
                             <Form.Group controlId="profilePicture">
                                <Form.Label>เปลี่ยนรูปโปรไฟล์</Form.Label>
                                <Form.Control type="file" onChange={handleFileChange} accept="image/*" />
                            </Form.Group>
                        </Col>
                    </Row>

                    <Row>
                        <Col md={12}>
                            <Form.Group className="mb-3" controlId="name">
                                <Form.Label>ชื่อ-นามสกุล</Form.Label>
                                <Form.Control
                                    type="text"
                                    name="name"
                                    value={formData.name}
                                    onChange={handleChange}
                                    required
                                />
                            </Form.Group>
                        </Col>
                       
                    </Row>
                    
                    <hr/>
                    <p className="text-muted">เปลี่ยนรหัสผ่าน (กรอกเฉพาะเมื่อต้องการเปลี่ยน)</p>
                     <Row>
                        <Col md={6}>
                            <Form.Group className="mb-3" controlId="password">
                                <Form.Label>รหัสผ่านใหม่</Form.Label>
                                <Form.Control
                                    type="password"
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                />
                            </Form.Group>
                        </Col>
                        <Col md={6}>
                            <Form.Group className="mb-3" controlId="confirmPassword">
                                <Form.Label>ยืนยันรหัสผ่านใหม่</Form.Label>
                                <Form.Control
                                    type="password"
                                    name="confirmPassword"
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                />
                            </Form.Group>
                        </Col>
                    </Row>

                    <div className="d-flex justify-content-end mt-4">
                        <Button as={Link} to="/profile" variant="secondary" className="me-2" disabled={submitting}>
                            <FaTimes className="me-1"/> ยกเลิก
                        </Button>
                        <Button variant="primary" type="submit" disabled={submitting}>
                            {submitting ? (
                                <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />
                            ) : (
                                <FaSave className="me-1" />
                            )}
                            {submitting ? ' กำลังบันทึก...' : ' บันทึกการเปลี่ยนแปลง'}
                        </Button>
                    </div>
                </Form>
            </Card.Body>
        </Card>
    );
};

export default ProfileEdit;