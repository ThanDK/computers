import React, { useState, useEffect } from 'react';
import { Card, Button, Spinner, Alert, ListGroup, Badge, Row, Col } from 'react-bootstrap'; 
import { FaPlus, FaEdit, FaTrash, FaCheckCircle } from 'react-icons/fa';
import { useAuth } from '../../context/AuthContext';
import AddressModal from './AddressModal'; 
import { 
    getUserAddresses, 
    createAddress, 
    updateAddress, 
    deleteAddress, 
    setDefaultAddress 
} from '../../services/AddressService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService.js';
import '../../Pages/ProfilePage/ProfilePage.css'; // Step 1.1: Fix CSS loading bug

const AddressPage = () => {
    const { user } = useAuth();
    const [addresses, setAddresses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [editingAddress, setEditingAddress] = useState(null);

    const fetchAddresses = async () => {
        if (!user) return;
        try {
            setLoading(true);
            setError(null);
            const response = await getUserAddresses();
            setAddresses(response.data);
        } catch (err) {
            console.error("Failed to fetch addresses:", err);
            setError("ไม่สามารถโหลดข้อมูลที่อยู่ได้");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAddresses();
    }, [user]); 

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
                await updateAddress(editingAddress.id, addressData);
                notifySuccess('อัปเดตที่อยู่เรียบร้อยแล้ว'); 
            } else {
                await createAddress(addressData);
                notifySuccess('เพิ่มที่อยู่ใหม่เรียบร้อยแล้ว'); 
            }
            handleCloseModal();
            fetchAddresses();
        } catch (err) {
            console.error("Failed to save address:", err);
            notifyError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการบันทึกที่อยู่'); 
        }
    };
    
    const handleDeleteAddress = async (addressId) => {
        const isConfirmed = await showConfirmation('ยืนยันการลบ', 'คุณแน่ใจหรือไม่ว่าต้องการลบที่อยู่นี้?');
        if (isConfirmed) {
            try {
                await deleteAddress(addressId);
                notifySuccess('ลบที่อยู่เรียบร้อยแล้ว'); 
                fetchAddresses(); 
            } catch (err) {
                console.error("Failed to delete address:", err);
                notifyError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการลบที่อยู่'); 
            }
        }
    };

    const handleSetDefault = async (addressId) => {
        try {
            await setDefaultAddress(addressId);
            notifySuccess('ตั้งค่าที่อยู่หลักเรียบร้อยแล้ว'); 
            fetchAddresses(); 
        } catch (err) {
            console.error("Failed to set default address:", err);
            notifyError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการตั้งค่าที่อยู่หลัก'); 
        }
    };

    if (loading) return <div className="text-center p-5"><Spinner animation="border" /></div>;
    if (error) return <Alert variant="danger">{error}</Alert>;

    // Step 1.2: Separate address data for the new layout
    const defaultAddress = addresses.find(addr => addr.isDefault);
    const otherAddresses = addresses.filter(addr => !addr.isDefault);

    // Reusable component for displaying a single address row
    const AddressRow = ({ addr, isDefault = false }) => (
        <Row>
            <Col md={8}>
                <strong className="d-block mb-2">{addr.contactName}</strong>
                <p className="text-muted mb-1">{addr.phoneNumber}</p>
                <p className="mb-0">{`${addr.line1}${addr.line2 ? `, ${addr.line2}` : ''}`}</p>
                <p className="mb-0">{`${addr.subdistrict}, ${addr.district}, ${addr.province} ${addr.zipCode}`}</p>
            </Col>
            <Col md={4} className="text-md-end mt-2 mt-md-0 d-flex flex-md-column justify-content-start align-items-md-end">
                <Button variant="outline-secondary" size="sm" className="mb-md-2 me-2 me-md-0" onClick={() => handleOpenModal(addr)}><FaEdit /> แก้ไข</Button>
                <Button variant="outline-danger" size="sm" className="mb-md-2 me-2 me-md-0" onClick={() => handleDeleteAddress(addr.id)}><FaTrash /> ลบ</Button>
                {!isDefault && (
                    <Button variant="outline-primary" size="sm" onClick={() => handleSetDefault(addr.id)}>ตั้งเป็นที่อยู่หลัก</Button>
                )}
            </Col>
        </Row>
    );

    return (
        // Step 1.3: New Two-Card Layout
        <>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h5 className="mb-0">ที่อยู่สำหรับจัดส่ง</h5>
                <Button variant="primary" onClick={() => handleOpenModal()}>
                    <FaPlus className="me-2" /> เพิ่มที่อยู่ใหม่
                </Button>
            </div>
            
            {defaultAddress && (
                <Card className="mb-4 default-address-card">
                    <Card.Header>
                        <FaCheckCircle className="me-2"/>ที่อยู่หลัก
                    </Card.Header>
                    <Card.Body>
                        <AddressRow addr={defaultAddress} isDefault={true} />
                    </Card.Body>
                </Card>
            )}

            {otherAddresses.length > 0 && (
                 <Card>
                    <Card.Header>ที่อยู่ที่บันทึกไว้</Card.Header>
                    <ListGroup variant="flush">
                        {otherAddresses.map(addr => (
                            <ListGroup.Item key={addr.id} className="p-3">
                                <AddressRow addr={addr} />
                            </ListGroup.Item>
                        ))}
                    </ListGroup>
                </Card>
            )}

            {addresses.length === 0 && (
                <Card>
                    <Card.Body className="text-center text-muted p-4">
                        ยังไม่มีที่อยู่ที่บันทึกไว้
                    </Card.Body>
                </Card>
            )}

            <AddressModal
                show={showModal}
                handleClose={handleCloseModal}
                handleSave={handleSaveAddress}
                address={editingAddress}
            />
        </>
    );
};

export default AddressPage;