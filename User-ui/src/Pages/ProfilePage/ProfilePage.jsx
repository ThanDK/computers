

import React from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import ProfileSidebar from '../../component/Profilesidebar/ProfileSidebar';
import { Outlet } from 'react-router-dom';
import './ProfilePage.css'; 

const ProfilePage = () => {
    return (
        <Container className="my-5">
            <Row>
                <Col md={4} lg={3}>
                    <ProfileSidebar />
                </Col>
                <Col md={8} lg={9}>
                    {}
                    <Outlet />
                </Col>
            </Row>
        </Container>
    );
};

export default ProfilePage;