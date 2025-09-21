

import React, { useState, useEffect } from 'react';
import { Card, ListGroup, Badge, Spinner } from 'react-bootstrap';
import { NavLink } from 'react-router-dom';
import { FaBox, FaHeart, FaUser, FaMapMarkerAlt, FaWrench } from 'react-icons/fa';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api'; 

const ProfileSidebar = () => {
    const { user } = useAuth();

    const [counts, setCounts] = useState({
        orders: 0,
        favorites: 0,
        builds: 0,
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user) {
            setLoading(false);
            setCounts({ orders: 0, favorites: 0, builds: 0 });
            return;
        }

        
        setLoading(false); 
        
        /*
        const fetchCounts = async () => {
            setLoading(true);
            try {
                const [ordersResponse, buildsResponse] = await Promise.all([
                    api.get('/api/v1/orders/my-orders').catch(e => ({ data: [] })), 
                    api.get('/api/v1/builds/my-builds').catch(e => ({ data: [] })),
                ]);
                
                const waitingStatuses = ['PENDING_PAYMENT', 'PAID', 'PROCESSING', 'PENDING_SHIPMENT'];
                const pendingOrdersCount = ordersResponse.data.filter(order => 
                    waitingStatuses.includes(order.status)
                ).length;

                setCounts({
                    orders: pendingOrdersCount,
                    favorites: 0, 
                    builds: buildsResponse.data.length,
                });

            } catch (error) {
                console.error("Failed to fetch sidebar counts:", error);
                setCounts({ orders: 0, favorites: 0, builds: 0 });
            } finally {
                setLoading(false);
            }
        };

        fetchCounts();
        */
    }, [user]);

    const renderBadge = (count) => {
        
        if (count > 0) {
            return <Badge bg="danger" pill>{count}</Badge>;
        }
        return null;
    };
    
   
    return (
        <>
            <Card className="mb-4">
                <Card.Header as="h6">รายการ</Card.Header>
                <ListGroup variant="flush">
                    <ListGroup.Item action as={NavLink} to="/profile/orders" className="d-flex justify-content-between align-items-center">
                        <span><FaBox className="me-2" /> สถานะคำสั่งซื้อ</span>
                        {renderBadge(counts.orders)}
                    </ListGroup.Item>
                  
                </ListGroup>
            </Card>

            <Card>
                <Card.Header as="h6">จัดการบัญชี</Card.Header>
                <ListGroup variant="flush">
                    <ListGroup.Item action as={NavLink} to="/profile" end>
                        <FaUser className="me-2" /> ข้อมูลส่วนตัว
                    </ListGroup.Item>
                    <ListGroup.Item action as={NavLink} to="/profile/address">
                        <FaMapMarkerAlt className="me-2" /> ที่อยู่สำหรับจัดส่ง
                    </ListGroup.Item>
                    <ListGroup.Item action as={NavLink} to="/profile/builds" className="d-flex justify-content-between align-items-center">
                        <span><FaWrench className="me-2" /> Build ที่สร้าง</span>
                        {renderBadge(counts.builds)}
                    </ListGroup.Item>
                </ListGroup>
            </Card>
        </>
    );
};

export default ProfileSidebar;