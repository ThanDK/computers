

import React, { useState, useEffect, useCallback } from 'react';
import { Card, Button, Spinner, Alert, ListGroup, Badge, Row, Col } from 'react-bootstrap'; 
import { FaPlus, FaEdit, FaTrash, FaCheckCircle } from 'react-icons/fa';
import api from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import AddressModal from './AddressModal'; 

const AddressPage = () => {
    const { user } = useAuth();
    const [addresses, setAddresses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [editingAddress, setEditingAddress] = useState(null);

    const fetchAddresses = useCallback(async () => {
        if (!user) return;
        try {
            setLoading(true);
            setError(null);
            
            const response = await api.get('/user/addresses');
            setAddresses(response.data);
        } catch (err) {
            console.error("Failed to fetch addresses:", err);
            setError("ไม่สามารถโหลดข้อมูลที่อยู่ได้");
        } finally {
            setLoading(false);
        }
    }, [user]);

    useEffect(() => {
        fetchAddresses();
    }, [fetchAddresses]);

    const handleOpenModal = (address = null) => {
        setEditingAddress(address);
        setShowModal(true);
    };

    const handleCloseModal = () => {
        setShowModal(false);
        setEditingAddress(null);
    };

    const handleSaveAddress = async (addressData) => {
        try {
            if (editingAddress) {
                await api.put(`/user/addresses/${editingAddress.id}`, addressData);
            } else {
                await api.post('/user/addresses', addressData);
            }
            fetchAddresses();
            handleCloseModal();
        } catch (err) {
            console.error("Failed to save address:", err);
            alert(`เกิดข้อผิดพลาดในการบันทึกที่อยู่: ${err.response?.data?.message || err.message}`);
        }
    };
    
    const handleDeleteAddress = async (addressId) => {
        if (window.confirm('คุณแน่ใจหรือไม่ว่าต้องการลบที่อยู่นี้?')) {
            try {
                await api.delete(`/user/addresses/${addressId}`);
                fetchAddresses();
            } catch (err) {
                console.error("Failed to delete address:", err);
                alert(`เกิดข้อผิดพลาดในการลบที่อยู่: ${err.response?.data?.message || err.message}`);
            }
        }
    };

    const handleSetDefault = async (addressId) => {
        try {
            await api.post(`/user/addresses/set-default/${addressId}`);
            fetchAddresses();
        } catch (err) {
            console.error("Failed to set default address:", err);
            alert(`เกิดข้อผิดพลาดในการตั้งค่าที่อยู่หลัก: ${err.response?.data?.message || err.message}`);
        }
    };

    if (loading) {
        return <div className="text-center p-5"><Spinner animation="border" /></div>;
    }

    if (error) {
        return <Alert variant="danger">{error}</Alert>;
    }

    return (
        <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
                <h5 className="mb-0">ที่อยู่สำหรับจัดส่ง</h5>
                <Button variant="primary" onClick={() => handleOpenModal()}>
                    <FaPlus className="me-2" /> เพิ่มที่อยู่ใหม่
                </Button>
            </Card.Header>
            <ListGroup variant="flush">
                {addresses.length === 0 ? (
                    <ListGroup.Item className="text-center text-muted p-4">
                        ยังไม่มีที่อยู่ที่บันทึกไว้
                    </ListGroup.Item>
                ) : (
                    addresses.map(addr => (
                        <ListGroup.Item key={addr.id} className="p-3">
                            <Row>
                                <Col md={8}>
                                    <div className="d-flex align-items-center mb-2">
                                        <strong className="me-3">{addr.contactName}</strong>
                                        {addr.isDefault && <Badge bg="success"><FaCheckCircle className="me-1"/> ที่อยู่หลัก</Badge>}
                                    </div>
                                    <p className="text-muted mb-1">{addr.phoneNumber}</p>
                                    <p className="mb-0">{`${addr.line1}${addr.line2 ? `, ${addr.line2}` : ''}`}</p>
                                    <p className="mb-0">{`${addr.subdistrict}, ${addr.district}, ${addr.province} ${addr.zipCode}`}</p>
                                </Col>
                                <Col md={4} className="text-md-end mt-2 mt-md-0 d-flex flex-md-column justify-content-start align-items-md-end">
                                     <Button variant="outline-secondary" size="sm" className="mb-md-2 me-2 me-md-0" onClick={() => handleOpenModal(addr)}><FaEdit /> แก้ไข</Button>
                                     <Button variant="outline-danger" size="sm" className="mb-md-2 me-2 me-md-0" onClick={() => handleDeleteAddress(addr.id)}><FaTrash /> ลบ</Button>
                                     {!addr.isDefault && (
                                         <Button variant="outline-primary" size="sm" onClick={() => handleSetDefault(addr.id)}>ตั้งเป็นที่อยู่หลัก</Button>
                                     )}
                                </Col>
                            </Row>
                        </ListGroup.Item>
                    ))
                )}
            </ListGroup>

            {}
            <AddressModal
                show={showModal}
                handleClose={handleCloseModal}
                handleSave={handleSaveAddress}
                address={editingAddress}
            />
        </Card>
    );
};

export default AddressPage;