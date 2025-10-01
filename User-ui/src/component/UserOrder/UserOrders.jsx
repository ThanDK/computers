import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Container, Card, Row, Col, Badge, Button, Spinner, Alert, Image, Tabs, Tab, Modal, Form } from 'react-bootstrap';
import { FaBoxOpen, FaShippingFast, FaHistory, FaCreditCard, FaReceipt, FaExclamationTriangle, FaUndo, FaUpload, FaWrench, FaChevronDown, FaChevronUp } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import { fetchMyOrders, cancelOrderByUser, retryPaypalPayment, resubmitSlip } from '../../services/OrderService';
import { notifySuccess, notifyError } from '../../services/NotificationService';
import { format } from 'date-fns';
import './UserOrders.css';

// ... getStatusBadge function is unchanged ...
const getStatusBadge = (order) => {
    if (order.paymentDetails?.paymentMethod === 'BANK_TRANSFER' && order.orderStatus !== 'REJECTED_SLIP') {
        const paymentStatusMap = {
            PENDING_APPROVAL: { text: 'รอตรวจสอบสลิป', variant: 'info' },
            APPROVED: { text: 'ชำระเงินแล้ว', variant: 'success' },
        };
        if (paymentStatusMap[order.paymentStatus]) {
            return paymentStatusMap[order.paymentStatus];
        }
    }
    
    const statusMap = {
        PENDING_PAYMENT: { text: 'รอการชำระเงิน', variant: 'warning' },
        REJECTED_SLIP: { text: 'สลิปถูกปฏิเสธ', variant: 'danger' },
        PROCESSING: { text: 'กำลังเตรียมจัดส่ง', variant: 'primary' },
        SHIPPED: { text: 'จัดส่งแล้ว', variant: 'info' },
        COMPLETED: { text: 'จัดส่งสำเร็จ', variant: 'success' },
        DELIVERY_FAILED: { text: 'จัดส่งไม่สำเร็จ', variant: 'danger' },
        RETURNED_TO_SENDER: { text: 'พัสดุตีกลับ', variant: 'warning' },
        CANCELLED: { text: 'ยกเลิกแล้ว', variant: 'danger' },
        REFUNDED: { text: 'คืนเงินแล้ว', variant: 'secondary' },
        REFUND_REQUESTED: { text: 'ขอคืนเงิน', variant: 'info' },
    };
    return statusMap[order.orderStatus] || { text: order.orderStatus, variant: 'light' };
};

const UserOrders = () => {
    // ... all states and other functions are unchanged ...
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('current');
    
    const [showSlipModal, setShowSlipModal] = useState(false);
    const [selectedSlipUrl, setSelectedSlipUrl] = useState('');

    const [showResubmitModal, setShowResubmitModal] = useState(false);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [newSlipFile, setNewSlipFile] = useState(null);
    const [isResubmitting, setIsResubmitting] = useState(false);
    const fileInputRef = useRef(null);
    const [expandedItems, setExpandedItems] = useState(new Set());

    const loadUserOrders = useCallback(async () => {
        setError(null);
        try {
            const response = await fetchMyOrders();
            const ordersArray = response.data;
            if (Array.isArray(ordersArray)) {
                const sortedOrders = ordersArray.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                setOrders(sortedOrders);
            } else { setOrders([]); }
        } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'เกิดข้อผิดพลาดในการดึงข้อมูล';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { 
        loadUserOrders(); 
    }, [loadUserOrders]);

    useEffect(() => {
        const intervalId = setInterval(() => {
            loadUserOrders();
        }, 3000); 
        return () => {
            clearInterval(intervalId);
        };
    }, [loadUserOrders]); 

    const handleCancelOrder = async (orderId) => {
        if (window.confirm('คุณต้องการยกเลิกคำสั่งซื้อนี้ใช่หรือไม่?')) {
            try {
                await cancelOrderByUser(orderId);
                notifySuccess('ยกเลิกคำสั่งซื้อสำเร็จ');
                loadUserOrders();
            } catch (err) {
                notifyError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการยกเลิก');
            }
        }
    };

    const handleRetryPayment = async (orderId) => {
        try {
            const response = await retryPaypalPayment(orderId);
            const approvalLink = response.data.approvalUrl || response.data.approvalLink;
            if (approvalLink) {
                window.location.href = approvalLink;
            } else {
                notifyError('ไม่สามารถสร้างลิงก์ชำระเงินได้ในขณะนี้');
            }
        } catch (err) {
            notifyError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการสร้างลิงก์ชำระเงิน');
        }
    };
    
    const handleShowSlip = (url) => {
        setSelectedSlipUrl(url);
        setShowSlipModal(true);
    };

    const handleOpenResubmitModal = (order) => {
        setSelectedOrder(order);
        setShowResubmitModal(true);
    };

    const handleCloseResubmitModal = () => {
        setShowResubmitModal(false);
        setSelectedOrder(null);
        setNewSlipFile(null);
        setIsResubmitting(false);
        if (fileInputRef.current) {
            fileInputRef.current.value = null;
        }
    };

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files[0]) {
            setNewSlipFile(e.target.files[0]);
        }
    };

    const handleResubmitSlip = async () => {
        if (!selectedOrder || !newSlipFile) {
            notifyError('กรุณาเลือกไฟล์สลิป');
            return;
        }
        setIsResubmitting(true);
        try {
            const orderId = selectedOrder.id || selectedOrder._id;
            await resubmitSlip(orderId, newSlipFile);
            notifySuccess('ส่งสลิปใหม่สำเร็จ กำลังรอการตรวจสอบ');
            handleCloseResubmitModal();
            loadUserOrders();
        } catch (err) {
            notifyError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการส่งสลิป');
        } finally {
            setIsResubmitting(false);
        }
    };
    
    const toggleItemExpansion = (lineItemId) => {
        setExpandedItems(prev => {
            const newSet = new Set(prev);
            if (newSet.has(lineItemId)) {
                newSet.delete(lineItemId);
            } else {
                newSet.add(lineItemId);
            }
            return newSet;
        });
    };

    const waitingStatuses = ['PENDING_PAYMENT', 'PROCESSING', 'REJECTED_SLIP', 'DELIVERY_FAILED', 'RETURNED_TO_SENDER'];
    const shippingStatuses = ['SHIPPED'];
    const historyStatuses = ['COMPLETED', 'CANCELLED', 'REFUNDED', 'REFUND_REQUESTED'];
    
    const isCancellable = (order) => {
        if (order.paymentDetails?.paymentMethod === 'BANK_TRANSFER') {
            return false;
        }
        return order.orderStatus === 'PENDING_PAYMENT';
    };
    
    const isPayable = (order) => 
        order.paymentDetails?.paymentMethod === 'PAYPAL' && 
        order.orderStatus === 'PENDING_PAYMENT';
        
    const canResubmitSlip = (order) => 
        order.orderStatus === 'REJECTED_SLIP' &&
        order.paymentDetails?.paymentMethod === 'BANK_TRANSFER';

    const waitingOrders = orders.filter(order => 
        waitingStatuses.includes(order.orderStatus) ||
        (order.paymentDetails?.paymentMethod === 'BANK_TRANSFER' && ['PENDING_APPROVAL'].includes(order.paymentStatus))
    );
    const shippingOrders = orders.filter(order => shippingStatuses.includes(order.orderStatus));
    const historyOrders = orders.filter(order => 
        historyStatuses.includes(order.orderStatus) || 
        (order.paymentDetails?.paymentMethod === 'BANK_TRANSFER' && order.paymentStatus === 'APPROVED' && !shippingStatuses.includes(order.orderStatus) && !waitingStatuses.includes(order.orderStatus))
    );
    
    const renderOrderCard = (order) => {
        const orderId = order.id || order._id;
        if (!order || !orderId) return null;

        const statusInfo = getStatusBadge(order);
        const isBankTransfer = order.paymentDetails?.paymentMethod === 'BANK_TRANSFER';

        return (
            <Card key={orderId} className={`order-item-card status-${statusInfo.variant}`}>
                <Card.Header className="d-flex justify-content-between align-items-center flex-wrap">
                    <div>
                        <strong>Order ID:</strong> #{(orderId.slice(-8).toUpperCase())}
                    </div>
                    <Badge bg={statusInfo.variant} text={['warning', 'info', 'light'].includes(statusInfo.variant) ? 'dark' : 'white'}>
                        {statusInfo.text}
                    </Badge>
                </Card.Header>
                <Card.Body>
                    <div className="text-muted small mb-2">
                        <strong>สั่งซื้อเมื่อ:</strong> {order.createdAt ? format(new Date(order.createdAt), 'dd MMM yyyy, HH:mm') : 'No Date'}
                    </div>

                    {/* Other alerts are unchanged... */}
                    {order.orderStatus === 'SHIPPED' && order.shippingDetails && (
                         <Alert variant="info" className="small py-2 mt-2">
                            <strong className="d-block mb-1"><FaShippingFast className="me-2" />ข้อมูลการจัดส่ง</strong>
                            <div><strong>ผู้ให้บริการ:</strong> {order.shippingDetails.shippingProvider || '-'}</div>
                            <div><strong>เลขพัสดุ:</strong> {order.shippingDetails.trackingNumber || '-'}</div>
                        </Alert>
                    )}

                    {isBankTransfer && order.orderStatus === 'REJECTED_SLIP' && (
                        <Alert variant="danger" className="small">
                            <strong><FaExclamationTriangle className="me-1" /> เหตุผลที่ถูกปฏิเสธ:</strong> {order.paymentDetails?.slipRejectionReason || 'ไม่มีเหตุผลระบุ'}
                            <p className="mb-0 mt-1">กรุณาตรวจสอบและส่งสลิปการชำระเงินใหม่อีกครั้ง</p>
                        </Alert>
                    )}

                    {(order.orderStatus === 'DELIVERY_FAILED' || order.orderStatus === 'RETURNED_TO_SENDER') && (
                        <Alert variant="warning" className="small">
                            {order.orderStatus === 'DELIVERY_FAILED' ? 
                                <><FaExclamationTriangle className="me-1" /> <strong>การจัดส่งมีปัญหา</strong></> : 
                                <><FaUndo className="me-1" /> <strong>พัสดุถูกตีกลับ</strong></>
                            }
                            <p className="mb-0 mt-1">กรุณาติดต่อเจ้าหน้าที่เพื่อดำเนินการแก้ไขที่อยู่หรือนัดหมายการจัดส่งใหม่อีกครั้ง</p>
                        </Alert>
                    )}


                    {order.lineItems && order.lineItems.length > 0 ? (
                        order.lineItems.map((item, index) => {
                            const lineItemId = item.id || item.productId || `${orderId}-${index}`;
                            const isBuild = item.itemType === 'BUILD';
                            const isExpanded = expandedItems.has(lineItemId);

                            return (
                                <div key={lineItemId} className="border-bottom py-2">
                                    <Row className="align-items-center">
                                        <Col xs={3} sm={2}>
                                            <div className="order-item-image-container">
                                                {isBuild ? (
                                                    <FaWrench className="build-icon" />
                                                ) : (
                                                    <Image src={item.imageUrl || 'https://via.placeholder.com/150'} fluid rounded />
                                                )}
                                            </div>
                                        </Col>
                                        <Col xs={9} sm={10}>
                                            <Row>
                                                <Col md={isBuild ? 8 : 12}>
                                                    <p className="mb-0 fw-bold">{item.name || item.productName || 'ไม่มีชื่อสินค้า'}</p>
                                                    <p className="text-muted small mb-0">
                                                        {Number(item.price || item.unitPrice || 0).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })} x {item.quantity}
                                                    </p>
                                                </Col>
                                                {isBuild && (
                                                     <Col md={4} className="text-md-end mt-2 mt-md-0">
                                                        <Button
                                                            variant="link"
                                                            size="sm"
                                                            className="p-0 expand-toggle-button"
                                                            onClick={() => toggleItemExpansion(lineItemId)}
                                                        >
                                                            รายละเอียด {isExpanded ? <FaChevronUp /> : <FaChevronDown />}
                                                        </Button>
                                                    </Col>
                                                )}
                                            </Row>
                                        </Col>
                                    </Row>
                                    
                                    {/* ======================= THE FIX IS HERE ======================= */}
                                    {isBuild && isExpanded && Array.isArray(item.containedItems) && item.containedItems.length > 0 && (
                                        <div className="component-list-container mt-2">
                                            {item.containedItems.map((component, compIndex) => (
                                                <div key={component.componentId || compIndex} className="component-item">
                                                    {/* จัดกลุ่มรูปภาพและชื่อไว้ด้วยกัน */}
                                                    <div className="component-info">
                                                        <div className="component-image-container">
                                                            <Image src={component.imageUrl || 'https://via.placeholder.com/50'} className="component-image" />
                                                        </div>
                                                        <span className="small">{component.name}</span>
                                                    </div>
                                                    {/* แสดงราคาทางด้านขวา */}
                                                    <span className="small component-price">
                                                        {Number(component.priceAtTimeOfOrder || 0).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })}
                                                    </span>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                    {/* =============================================================== */}

                                </div>
                            );
                        })
                    ) : (
                        <div className="text-center text-muted py-3">ไม่มีรายการสินค้า</div>
                    )}
                </Card.Body>
                {/* ... Card.Footer is unchanged ... */}
                 <Card.Footer>
                    <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <span className="fw-bold fs-5">
                            ยอดรวม: {Number(order.totalAmount).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })}
                        </span>
                        <div className="d-flex gap-2 flex-wrap justify-content-end">
                           {isBankTransfer && order.paymentDetails?.slipImageUrl && (
                                <Button variant="outline-info" size="sm" onClick={() => handleShowSlip(order.paymentDetails.slipImageUrl)}>
                                    <FaReceipt className="me-1" /> ดูสลิป
                                </Button>
                           )}
                           {canResubmitSlip(order) && (
                                <Button variant="warning" size="sm" onClick={() => handleOpenResubmitModal(order)}>
                                    <FaUpload className="me-1" /> ส่งสลิปใหม่
                                </Button>
                           )}
                           {isPayable(order) && (
                                <Button variant="success" size="sm" onClick={() => handleRetryPayment(orderId)}>
                                    <FaCreditCard className="me-1" /> ชำระเงิน
                                </Button>
                           )}
                           {isCancellable(order) && (
                                <Button variant="outline-danger" size="sm" onClick={() => handleCancelOrder(orderId)}>
                                    ยกเลิก
                                </Button>
                           )}
                        </div>
                    </div>
                </Card.Footer>
            </Card>
        );
    };

    // ... The main return block with Tabs and Modals is unchanged ...
     return (
        <Container fluid>
            <h3 className="mb-4">คำสั่งซื้อของฉัน</h3>
            
            {loading && <div className="text-center my-5"><Spinner animation="border" /></div>}
            {!loading && error && <Alert variant="danger">{error}</Alert>}
            {!loading && !error && orders.length === 0 && <Alert variant="info">คุณยังไม่มีคำสั่งซื้อ</Alert>}

            {!loading && !error && orders.length > 0 && (
                 <Tabs id="user-orders-tabs" activeKey={activeTab} onSelect={(k) => setActiveTab(k)} className="mb-3" fill>
                    <Tab 
                        eventKey="current" 
                        title={<><FaBoxOpen className="me-2" />คำสั่งซื้อปัจจุบัน{(waitingOrders.length + shippingOrders.length) > 0 && <Badge pill bg="primary" className="ms-2">{waitingOrders.length + shippingOrders.length}</Badge>}</>}
                    >
                        <div className="pt-3">
                            {waitingOrders.length === 0 && shippingOrders.length === 0 ? (<Alert variant="info">ไม่มีคำสั่งซื้อปัจจุบัน</Alert>) : (
                                <>
                                    {shippingOrders.length > 0 && (
                                        <div className="mb-4">
                                            <h5 className="mb-3"><FaShippingFast className="me-2" />กำลังจัดส่ง ({shippingOrders.length})</h5>
                                            {shippingOrders.map(renderOrderCard)}
                                        </div>
                                    )}
                                    {waitingOrders.length > 0 && (
                                        <div className="mb-4">
                                            <h5 className="mb-3">ที่ต้องดำเนินการ ({waitingOrders.length})</h5>
                                            {waitingOrders.map(renderOrderCard)}
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                    </Tab>
                    <Tab 
                        eventKey="history" 
                        title={<><FaHistory className="me-2" />ประวัติคำสั่งซื้อ{historyOrders.length > 0 && <Badge pill bg="secondary" className="ms-2">{historyOrders.length}</Badge>}</>}
                    >
                         <div className="pt-3">
                            {historyOrders.length > 0 ? (historyOrders.map(renderOrderCard)) : (<Alert variant="info">ไม่มีประวัติคำสั่งซื้อ</Alert>)}
                        </div>
                    </Tab>
                </Tabs>
            )}

            <Modal show={showSlipModal} onHide={() => setShowSlipModal(false)} centered size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>สลิปการโอนเงิน</Modal.Title>
                </Modal.Header>
                <Modal.Body className="text-center">
                    {selectedSlipUrl ? (<Image src={selectedSlipUrl} fluid />) : (<p>ไม่พบรูปภาพ</p>)}
                </Modal.Body>
            </Modal>

            <Modal show={showResubmitModal} onHide={handleCloseResubmitModal} centered>
                <Modal.Header closeButton>
                    <Modal.Title>ส่งสลิปการชำระเงินใหม่</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {selectedOrder && (
                        <>
                            <p><strong>Order ID:</strong> #{(selectedOrder.id || selectedOrder._id).slice(-8).toUpperCase()}</p>
                            <p><strong>ยอดที่ต้องชำระ:</strong> {Number(selectedOrder.totalAmount).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })}</p>
                            <hr/>
                            <Form.Group controlId="formFile" className="mb-3">
                                <Form.Label>อัปโหลดสลิปใหม่</Form.Label>
                                <Form.Control 
                                    type="file" 
                                    accept="image/*,.pdf"
                                    onChange={handleFileChange}
                                    ref={fileInputRef}
                                />
                            </Form.Group>
                            {newSlipFile && (
                                <Alert variant="info" className="small py-2">
                                    ไฟล์ที่เลือก: {newSlipFile.name}
                                </Alert>
                            )}
                        </>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleCloseResubmitModal} disabled={isResubmitting}>
                        ปิด
                    </Button>
                    <Button 
                        variant="primary" 
                        onClick={handleResubmitSlip} 
                        disabled={!newSlipFile || isResubmitting}
                    >
                        {isResubmitting ? (
                            <><Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true"/> กำลังส่ง...</>
                        ) : (
                            'ยืนยันการส่ง'
                        )}
                    </Button>
                </Modal.Footer>
            </Modal>
        </Container>
    );
};

export default UserOrders;