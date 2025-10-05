import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Container, Card, Row, Col, Badge, Button, Spinner, Alert, Image, Tabs, Tab, Modal, Form } from 'react-bootstrap';
import { FaBoxOpen, FaShippingFast, FaHistory, FaCreditCard, FaReceipt, FaExclamationTriangle, FaUndo, FaUpload, FaWrench, FaChevronDown, FaChevronUp, FaQuestionCircle, FaSync } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import { fetchMyOrders, cancelOrderByUser, retryPaypalPayment, resubmitSlip, requestRefund, fetchDefaultPaymentMethod } from '../../services/OrderService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';
import { format } from 'date-fns';
import './UserOrders.css';

const getStatusBadge = (order) => {
    const statusMap = {
        PENDING_PAYMENT: { text: 'รอการชำระเงิน', variant: 'warning' },
        PENDING_APPROVAL: { text: 'รอตรวจสอบสลิป', variant: 'info' },
        REJECTED_SLIP: { text: 'สลิปถูกปฏิเสธ', variant: 'danger' },
        PROCESSING: { text: 'กำลังเตรียมจัดส่ง', variant: 'primary' },
        SHIPPED: { text: 'จัดส่งแล้ว', variant: 'info' },
        COMPLETED: { text: 'จัดส่งสำเร็จ', variant: 'success' },
        DELIVERY_FAILED: { text: 'จัดส่งไม่สำเร็จ', variant: 'danger' },
        RETURNED_TO_SENDER: { text: 'พัสดุตีกลับ', variant: 'warning' },
        CANCELLED: { text: 'ยกเลิกแล้ว', variant: 'secondary' },
        REFUND_REQUESTED: { text: 'กำลังดำเนินการคืนเงิน', variant: 'info' },
        REFUND_REJECTED: { text: 'คำขอคืนเงินถูกปฏิเสธ', variant: 'danger'},
        REFUNDED: { text: 'คืนเงินแล้ว', variant: 'success' },
    };
    
    if (order.paymentDetails?.paymentMethod === 'BANK_TRANSFER' && order.paymentStatus === 'PENDING_APPROVAL') {
        return statusMap['PENDING_APPROVAL'];
    }

    return statusMap[order.orderStatus] || { text: order.orderStatus, variant: 'light' };
};

const UserOrders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('action');
    
    const [showSlipModal, setShowSlipModal] = useState(false);
    const [slipModalTitle, setSlipModalTitle] = useState('');
    const [selectedSlipUrl, setSelectedSlipUrl] = useState('');

    const [showResubmitModal, setShowResubmitModal] = useState(false);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [newSlipFile, setNewSlipFile] = useState(null);
    const [isResubmitting, setIsResubmitting] = useState(false);
    const fileInputRef = useRef(null);
    const [expandedItems, setExpandedItems] = useState(new Set());

    const [bankDetails, setBankDetails] = useState(null);
    const [isBankDetailsLoading, setIsBankDetailsLoading] = useState(false);
    const [bankDetailsError, setBankDetailsError] = useState(null);


    // FIX: Accept a parameter to differentiate between manual and background refresh
    const loadUserOrders = useCallback(async (isManualRefresh = false) => {
        // FIX: Only show the main loading spinner for the initial load or a manual refresh
        if (isManualRefresh) {
            setLoading(true);
        } else if (orders.length === 0) { // Keep spinner for very first load
             setLoading(true);
        }
        
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
    }, [orders.length]); // Add dependency to re-evaluate the function if orders length changes

    useEffect(() => { 
        loadUserOrders(); 
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // Run only once on mount for initial load

    useEffect(() => {
        const intervalId = setInterval(() => {
            loadUserOrders(false); // Call with false for silent background refresh
        }, 30000);
        return () => {
            clearInterval(intervalId);
        };
    }, [loadUserOrders]); 

    const handleCancelOrder = async (orderId) => {
        const confirmed = await showConfirmation('ยืนยันการยกเลิก', 'คุณต้องการยกเลิกคำสั่งซื้อนี้ใช่หรือไม่?');
        if (confirmed) {
            try {
                await cancelOrderByUser(orderId);
                notifySuccess('ยกเลิกคำสั่งซื้อสำเร็จ');
                loadUserOrders(true); // Manually refresh after action
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
    
    const handleRequestRefund = async (orderId) => {
        const confirmed = await showConfirmation('ยืนยันการขอคืนเงิน', 'คุณต้องการส่งคำขอคืนเงินสำหรับคำสั่งซื้อนี้ใช่หรือไม่?');
        if (confirmed) {
            try {
                await requestRefund(orderId);
                notifySuccess('ส่งคำขอคืนเงินสำเร็จ');
                loadUserOrders(true); // Manually refresh after action
            } catch (err) {
                notifyError(err.response?.data?.message || 'เกิดข้อผิดพลาดในการส่งคำขอคืนเงิน');
            }
        }
    };

    const handleShowSlip = (url, type) => {
        setSlipModalTitle(type === 'refund' ? 'สลิปการคืนเงิน' : 'สลิปการโอนเงิน');
        setSelectedSlipUrl(url);
        setShowSlipModal(true);
    };

    const fetchBankDetailsForModal = async () => {
        setIsBankDetailsLoading(true);
        setBankDetailsError(null);
        try {
            const response = await fetchDefaultPaymentMethod();
            setBankDetails(response.data);
        } catch (err) {
            setBankDetailsError("ไม่สามารถโหลดข้อมูลการชำระเงินได้");
        } finally {
            setIsBankDetailsLoading(false);
        }
    };
    
    const handleOpenResubmitModal = (order) => {
        setSelectedOrder(order);
        setShowResubmitModal(true);
        fetchBankDetailsForModal();
    };

    const handleCloseResubmitModal = () => {
        setShowResubmitModal(false);
        setSelectedOrder(null);
        setNewSlipFile(null);
        setIsResubmitting(false);
        setBankDetails(null);
        setBankDetailsError(null);
        setIsBankDetailsLoading(false);
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
            loadUserOrders(true); // Manually refresh after action
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

    const isCancellable = (order) => order.paymentDetails?.paymentMethod === 'PAYPAL' && order.orderStatus === 'PENDING_PAYMENT';
    const isPayable = (order) => order.paymentDetails?.paymentMethod === 'PAYPAL' && order.orderStatus === 'PENDING_PAYMENT';
    const canResubmitSlip = (order) => order.orderStatus === 'REJECTED_SLIP';
    const canRequestRefund = (order) => ['PROCESSING', 'DELIVERY_FAILED', 'RETURNED_TO_SENDER'].includes(order.orderStatus);

    const actionRequiredStatuses = ['PENDING_PAYMENT', 'REJECTED_SLIP'];
    const inProgressStatuses = ['PENDING_APPROVAL', 'PROCESSING', 'SHIPPED', 'DELIVERY_FAILED', 'RETURNED_TO_SENDER', 'REFUND_REQUESTED'];
    const historyStatuses = ['COMPLETED', 'CANCELLED', 'REFUND_REJECTED', 'REFUNDED'];

    const actionRequiredOrders = orders.filter(o => actionRequiredStatuses.includes(o.orderStatus));
    const inProgressOrders = orders.filter(o => inProgressStatuses.includes(o.orderStatus) || (o.paymentDetails?.paymentMethod === 'BANK_TRANSFER' && o.paymentStatus === 'PENDING_APPROVAL'));
    const historyOrders = orders.filter(o => historyStatuses.includes(o.orderStatus));
    
    const renderOrderCard = (order) => {
        const orderId = order.id || order._id;
        if (!order || !orderId) return null;

        const statusInfo = getStatusBadge(order);
        const isBankTransfer = order.paymentDetails?.paymentMethod === 'BANK_TRANSFER';

        return (
            <Card key={orderId} className={`order-item-card status-${statusInfo.variant}`}>
                <Card.Header className="d-flex justify-content-between align-items-center flex-wrap">
                    <div><strong>Order ID:</strong> #{(orderId.slice(-8).toUpperCase())}</div>
                    <Badge bg={statusInfo.variant} text={['warning', 'info', 'light'].includes(statusInfo.variant) ? 'dark' : 'white'}>{statusInfo.text}</Badge>
                </Card.Header>
                <Card.Body>
                    <div className="text-muted small mb-2">
                        <strong>สั่งซื้อเมื่อ:</strong> {order.createdAt ? format(new Date(order.createdAt), 'dd MMM yyyy, HH:mm') : 'No Date'}
                    </div>

                    {order.orderStatus === 'SHIPPED' && order.shippingDetails && (
                         <Alert variant="info" className="py-2 mt-2 d-flex align-items-center gap-3">
                            <div className="shipping-logo-container flex-shrink-0">
                                {order.shippingDetails.shippingProviderLogoUrl ? (
                                    <Image src={order.shippingDetails.shippingProviderLogoUrl} className="shipping-logo" />
                                ) : (
                                    <FaShippingFast className="shipping-logo-placeholder" />
                                )}
                            </div>
                            <div>
                                <strong className="d-block mb-1">ข้อมูลการจัดส่ง</strong>
                                <div><strong>ผู้ให้บริการ:</strong> {order.shippingDetails.shippingProvider || '-'}</div>
                                <div><strong>เลขพัสดุ:</strong> {order.shippingDetails.trackingNumber || '-'}</div>
                            </div>
                        </Alert>
                    )}

                    {isBankTransfer && order.orderStatus === 'REJECTED_SLIP' && (
                        <Alert variant="danger" className="small">
                            <strong><FaExclamationTriangle className="me-1" /> เหตุผลที่ถูกปฏิเสธ:</strong> {order.paymentDetails?.slipRejectionReason || 'ไม่มีเหตุผลระบุ'}
                            <p className="mb-0 mt-1">กรุณาตรวจสอบและส่งสลิปการชำระเงินใหม่อีกครั้ง</p>
                        </Alert>
                    )}
                    
                    {order.orderStatus === 'REFUND_REJECTED' && (
                         <Alert variant="danger" className="small">
                            <strong><FaExclamationTriangle className="me-1" /> คำขอคืนเงินถูกปฏิเสธ:</strong> กรุณาติดต่อเจ้าหน้าที่สำหรับข้อมูลเพิ่มเติม
                        </Alert>
                    )}

                    {(order.orderStatus === 'DELIVERY_FAILED' || order.orderStatus === 'RETURNED_TO_SENDER') && (
                        <Alert variant="warning" className="small">
                            {order.orderStatus === 'DELIVERY_FAILED' ? <><FaExclamationTriangle className="me-1" /> <strong>การจัดส่งมีปัญหา</strong></> : <><FaUndo className="me-1" /> <strong>พัสดุถูกตีกลับ</strong></>}
                            <p className="mb-0 mt-1">กรุณาติดต่อเจ้าหน้าที่เพื่อดำเนินการแก้ไขที่อยู่หรือนัดหมายการจัดส่งใหม่อีกครั้ง</p>
                        </Alert>
                    )}

                    {order.lineItems?.map((item, index) => {
                        const lineItemId = item.id || item.productId || `${orderId}-${index}`;
                        const isBuild = item.itemType === 'BUILD';
                        const isExpanded = expandedItems.has(lineItemId);
                        return (
                            <div key={lineItemId} className="border-bottom py-2">
                                <Row className="align-items-center">
                                    <Col xs={3} sm={2}>
                                        <div className="order-item-image-container">
                                            {isBuild ? <FaWrench className="build-icon" /> : <Image src={item.imageUrl || 'https://via.placeholder.com/150'} fluid rounded />}
                                        </div>
                                    </Col>
                                    <Col xs={9} sm={10}>
                                        <Row>
                                            <Col md={isBuild ? 8 : 12}>
                                                <p className="mb-0 fw-bold">{item.name || item.productName || 'ไม่มีชื่อสินค้า'}</p>
                                                <p className="text-muted small mb-0">{Number(item.price || item.unitPrice || 0).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })} x {item.quantity}</p>
                                            </Col>
                                            {isBuild && (<Col md={4} className="text-md-end mt-2 mt-md-0"><Button variant="link" size="sm" className="p-0 expand-toggle-button" onClick={() => toggleItemExpansion(lineItemId)}>รายละเอียด {isExpanded ? <FaChevronUp /> : <FaChevronDown />}</Button></Col>)}
                                        </Row>
                                    </Col>
                                </Row>
                                {isBuild && isExpanded && Array.isArray(item.containedItems) && item.containedItems.length > 0 && (
                                    <div className="component-list-container mt-2">
                                        {item.containedItems.map((component, compIndex) => (<div key={component.componentId || compIndex} className="component-item"><div className="component-info"><div className="component-image-container"><Image src={component.imageUrl || 'https://via.placeholder.com/50'} className="component-image" /></div><span className="small">{component.name}</span></div><span className="small component-price">{Number(component.priceAtTimeOfOrder || 0).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })}</span></div>))}
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </Card.Body>
                 <Card.Footer>
                    <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <span className="fw-bold fs-5">ยอดรวม: {Number(order.totalAmount).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })}</span>
                        <div className="d-flex gap-2 flex-wrap justify-content-end">
                           {isBankTransfer && order.paymentDetails?.slipImageUrl && (<Button variant="outline-secondary" size="sm" className="btn-view-slip" onClick={() => handleShowSlip(order.paymentDetails.slipImageUrl, 'payment')}><FaReceipt className="me-1" /> ดูสลิปชำระเงิน</Button>)}
                           {order.orderStatus === 'REFUNDED' && order.paymentDetails?.refundSlipUrl && (<Button variant="outline-info" size="sm" className="btn-view-refund-slip" onClick={() => handleShowSlip(order.paymentDetails.refundSlipUrl, 'refund')}><FaReceipt className="me-1" /> ดูสลิปคืนเงิน</Button>)}
                           {canResubmitSlip(order) && (<Button variant="warning" size="sm" onClick={() => handleOpenResubmitModal(order)}><FaUpload className="me-1" /> ส่งสลิปใหม่</Button>)}
                           {canRequestRefund(order) && (<Button variant="outline-warning" size="sm" className="btn-request-refund" onClick={() => handleRequestRefund(orderId)}><FaQuestionCircle className="me-1" /> ขอคืนเงิน</Button>)}
                           {isPayable(order) && (<Button variant="success" size="sm" onClick={() => handleRetryPayment(orderId)}><FaCreditCard className="me-1" /> ชำระเงิน</Button>)}
                           {isCancellable(order) && (<Button variant="outline-danger" size="sm" onClick={() => handleCancelOrder(orderId)}>ยกเลิก</Button>)}
                        </div>
                    </div>
                </Card.Footer>
            </Card>
        );
    };

     return (
        <Container fluid>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h3 className="mb-0">คำสั่งซื้อของฉัน</h3>
                <Button
                    variant="outline-secondary"
                    size="sm"
                    onClick={() => loadUserOrders(true)} // FIX: Call with true for manual refresh
                    disabled={loading}
                >
                    {loading ? (
                        <>
                            <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" className="me-2" />
                            กำลังโหลด...
                        </>
                    ) : (
                        <>
                            <FaSync className="me-2" />
                            รีเฟรช
                        </>
                    )}
                </Button>
            </div>
            
            {loading ? ( // Display main spinner only on loading state
                <div className="text-center my-5"><Spinner animation="border" /></div>
            ) : error ? (
                <Alert variant="danger">{error}</Alert>
            ) : orders.length === 0 ? (
                <Alert variant="info">คุณยังไม่มีคำสั่งซื้อ</Alert>
            ) : (
                 <Tabs id="user-orders-tabs" activeKey={activeTab} onSelect={(k) => setActiveTab(k)} className="mb-3" fill>
                    <Tab 
                        eventKey="action" 
                        title={<> <FaExclamationTriangle className="me-2" /> ต้องดำเนินการ {actionRequiredOrders.length > 0 && <Badge pill bg="danger" className="ms-2">{actionRequiredOrders.length}</Badge>} </>}
                    >
                        <div className="pt-3">
                           {actionRequiredOrders.length > 0 ? actionRequiredOrders.map(renderOrderCard) : <Alert variant="success">ไม่มีรายการที่ต้องดำเนินการ</Alert>}
                        </div>
                    </Tab>
                    <Tab 
                        eventKey="progress" 
                        title={<> <FaShippingFast className="me-2" /> กำลังดำเนินการ {inProgressOrders.length > 0 && <Badge pill bg="primary" className="ms-2">{inProgressOrders.length}</Badge>} </>}
                    >
                        <div className="pt-3">
                            {inProgressOrders.length > 0 ? inProgressOrders.map(renderOrderCard) : <Alert variant="info">ไม่มีรายการที่กำลังดำเนินการ</Alert>}
                        </div>
                    </Tab>
                    <Tab 
                        eventKey="history" 
                        title={<> <FaHistory className="me-2" /> ประวัติ {historyOrders.length > 0 && <Badge pill bg="secondary" className="ms-2">{historyOrders.length}</Badge>} </>}
                    >
                         <div className="pt-3">
                            {historyOrders.length > 0 ? historyOrders.map(renderOrderCard) : <Alert variant="info">ไม่มีประวัติคำสั่งซื้อ</Alert>}
                        </div>
                    </Tab>
                </Tabs>
            )}

            <Modal show={showSlipModal} onHide={() => setShowSlipModal(false)} centered size="lg">
                <Modal.Header closeButton><Modal.Title>{slipModalTitle}</Modal.Title></Modal.Header>
                <Modal.Body className="text-center">{selectedSlipUrl ? (<Image src={selectedSlipUrl} fluid />) : (<p>ไม่พบรูปภาพ</p>)}</Modal.Body>
            </Modal>

            <Modal show={showResubmitModal} onHide={handleCloseResubmitModal} centered>
                <Modal.Header closeButton><Modal.Title>ส่งสลิปการชำระเงินใหม่</Modal.Title></Modal.Header>
                <Modal.Body>
                    {isBankDetailsLoading ? (
                        <div className="text-center p-4"><Spinner animation="border" /></div>
                    ) : bankDetailsError ? (
                        <Alert variant="danger">{bankDetailsError}</Alert>
                    ) : bankDetails && (
                        <div className="payment-details-modal mb-3">
                            <div className="payment-details-header">THAI QR PAYMENT</div>
                            <div className="payment-details-body">
                                <Image src={bankDetails.qrCodeImageUrl} fluid className="qr-modal-image" />
                                <div className="bank-info-modal">
                                    <p><span>Account Name:</span> <strong>{bankDetails.accountName}</strong></p>
                                    <p><span>Bank:</span> <strong>{bankDetails.bankName}</strong></p>
                                    <p><span>Account No:</span> <strong>{bankDetails.accountNumber}</strong></p>
                                </div>
                            </div>
                        </div>
                    )}
                    
                    {selectedOrder && (
                        <>
                            <hr />
                            <p className="mt-3"><strong>Order ID:</strong> #{(selectedOrder.id || selectedOrder._id).slice(-8).toUpperCase()}</p>
                            <p><strong>ยอดที่ต้องชำระ:</strong> {Number(selectedOrder.totalAmount).toLocaleString('th-TH', { style: 'currency', currency: 'THB' })}</p>
                            <Form.Group controlId="formFile" className="mb-3">
                                <Form.Label>อัปโหลดสลิปใหม่</Form.Label>
                                <Form.Control type="file" accept="image/*,.pdf" onChange={handleFileChange} ref={fileInputRef}/>
                            </Form.Group>
                            {newSlipFile && (<Alert variant="info" className="small py-2">ไฟล์ที่เลือก: {newSlipFile.name}</Alert>)}
                        </>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleCloseResubmitModal} disabled={isResubmitting}>ปิด</Button>
                    <Button variant="primary" onClick={handleResubmitSlip} disabled={!newSlipFile || isResubmitting}>
                        {isResubmitting ? (<><Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true"/> กำลังส่ง...</>) : ('ยืนยันการส่ง')}
                    </Button>
                </Modal.Footer>
            </Modal>
        </Container>
    );
};

export default UserOrders;