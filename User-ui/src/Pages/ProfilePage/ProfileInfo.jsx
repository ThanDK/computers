import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Button, Spinner, Alert, Image } from 'react-bootstrap';
import { FaUserCircle, FaEdit } from 'react-icons/fa';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api';

const ProfileInfo = () => {
    const { user, loading: authLoading } = useAuth(); 

    const [profileData, setProfileData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchUserProfile = async () => {
            if (!user) {
                setLoading(false);
                return;
            }
            try {
                setLoading(true);
                setError(null);
                
                const response = await api.get('profile/me'); 
                setProfileData(response.data);

            } catch (err) {
                console.error("Failed to fetch user profile:", err);
                if (err.response) {
                    setError(`ไม่สามารถโหลดข้อมูลได้: ${err.response.status} ${err.response.data.message || ''}`);
                } else {
                    setError("ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาลองใหม่อีกครั้ง");
                }
            } finally {
                setLoading(false);
            }
        };

        if (!authLoading) {
            fetchUserProfile();
        }

    }, [user, authLoading]);

    if (authLoading || loading) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '200px' }}>
                <Spinner animation="border" role="status">
                    <span className="visually-hidden">Loading...</span>
                </Spinner>
            </div>
        );
    }

    if (error) {
        return <Alert variant="danger">{error}</Alert>;
    }

    if (!profileData) {
        return <Alert variant="warning">ไม่พบข้อมูลผู้ใช้</Alert>;
    }
    
    return (
        <Card className="p-4">
            <Card.Body>
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <h4 className="mb-0">ข้อมูลส่วนตัว</h4>
                    <Button as={Link} to="/profile/edit" variant="primary">
                        <FaEdit className="me-1" /> แก้ไขข้อมูลส่วนตัว
                    </Button>
                </div>
                <Card className="bg-light border-0 mb-4">
                    <Card.Body>
                        <Row className="align-items-center">
                            <Col xs="auto" className="pe-0">
                                {profileData.profilePictureUrl ? (
                                    <Image 
                                        src={profileData.profilePictureUrl} 
                                        alt="Profile" 
                                        roundedCircle 
                                        style={{ width: '70px', height: '70px', objectFit: 'cover' }} 
                                    />
                                ) : (
                                    <FaUserCircle size={70} className="text-secondary" />
                                )}
                            </Col>
                            <Col>
                                <h5 className="mb-1">{profileData.name}</h5>
                                <p className="text-muted mb-0">{profileData.email}</p>
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
                <div>
                    <Row className="py-2">
                        <Col sm={4} className="text-muted">ชื่อ-นามสกุล</Col>
                        <Col sm={8}>{profileData.name}</Col>
                    </Row>
                    <hr className="my-1"/>
                    <Row className="py-2">
                        <Col sm={4} className="text-muted">Email</Col>
                        <Col sm={8}>{profileData.email}</Col>
                    </Row>
                    <hr className="my-1"/>
                    <Row className="py-2">
                        <Col sm={4} className="text-muted">Role</Col>
                        <Col sm={8}>{profileData.role}</Col>
                    </Row>
                </div>
            </Card.Body>
        </Card>
    );
};

export default ProfileInfo;