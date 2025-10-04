import React, { useState, useRef, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Image, Form, Spinner, Alert, OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { createOrder, submitSlip, fetchDefaultPaymentMethod } from '../../services/OrderService';
import { getUserAddresses, createAddress, updateAddress } from '../../services/AddressService';
import { notifySuccess, notifyError } from '../../services/NotificationService';
import AddressModal from '../../component/Address/AddressModal';
import { FaPlus, FaMinus, FaTrash, FaUpload, FaCheckCircle, FaWrench, FaPlusCircle, FaEdit } from 'react-icons/fa';
import { BsCartX } from 'react-icons/bs';
import styles from './CartPage.module.css';

const CartPage = () => {
    const { cartItems, removeFromCart, updateQuantity, totalAmount, clearCart, isLoading: isCartLoading, isUpdating, updatingItemId } = useCart();
    const { user, token, isLoading: isAuthLoading } = useAuth();
    const navigate = useNavigate();

    const [step, setStep] = useState(1);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState(null);
    const [savedAddressId, setSavedAddressId] = useState('');
    const [userAddresses, setUserAddresses] = useState([]);
    const [isAddressLoading, setIsAddressLoading] = useState(true);
    const [paymentMethod, setPaymentMethod] = useState('PAYPAL');
    const [paymentSlip, setPaymentSlip] = useState(null);
    const fileInputRef = useRef(null);

    const [bankDetails, setBankDetails] = useState(null);
    const [isBankDetailsLoading, setIsBankDetailsLoading] = useState(false);
    
    const [showAddressModal, setShowAddressModal] = useState(false);
    const [editingAddress, setEditingAddress] = useState(null);

    const fetchAddresses = async (selectAddressId = null) => {
        if (user && token) {
            try {
                setIsAddressLoading(true);
                const response = await getUserAddresses();
                const addresses = response.data;

                addresses.sort((a, b) => b.isDefault - a.isDefault);

                setUserAddresses(addresses);

                if (selectAddressId) {
                    setSavedAddressId(selectAddressId);
                    return;
                }
                
                if (addresses.length > 0) {
                    setSavedAddressId(addresses[0].id);
                } else {
                    setSavedAddressId('');
                }
            } catch (error) {
                console.error("Failed to fetch addresses:", error);
            } finally {
                setIsAddressLoading(false);
            }
        } else {
            setIsAddressLoading(false);
        }
    };
    
    useEffect(() => {
        if (step === 2 && !isAuthLoading) {
            fetchAddresses();
        }
    }, [step, user, token, isAuthLoading]);
    
    useEffect(() => {
        const loadBankDetails = async () => {
            if (step === 3 && paymentMethod === 'BANK_TRANSFER') {
                setIsBankDetailsLoading(true);
                try {
                    const response = await fetchDefaultPaymentMethod();
                    setBankDetails(response.data);
                } catch (error) {
                    console.error("Failed to fetch bank details:", error);
                    setSubmitError("Could not load payment details. Please try again later.");
                } finally {
                    setIsBankDetailsLoading(false);
                }
            }
        };
        loadBankDetails();
    }, [step, paymentMethod]);


    const handleOpenAddressModal = (addressToEdit = null) => {
        setEditingAddress(addressToEdit);
        setShowAddressModal(true);
    };

    const handleSaveAddress = async (addressData) => {
        try {
            let savedAddress;
            if (editingAddress) {
                const response = await updateAddress(editingAddress.id, addressData);
                savedAddress = response.data;
                notifySuccess("Address updated successfully!");
            } else {
                const response = await createAddress(addressData);
                savedAddress = response.data;
                notifySuccess("New address added successfully!");
            }
            setShowAddressModal(false);
            setEditingAddress(null);
            
            await fetchAddresses(savedAddress.id);

        } catch (error) {
            console.error("Failed to save address:", error);
            const errorMessage = error.response?.data?.message || "An error occurred while saving the address.";
            notifyError(errorMessage);
        }
    };

    const handleStepClick = (targetStep) => {
        if (targetStep < step) {
            setStep(targetStep);
        }
    };

    const handleProceedToStep2 = () => setStep(2);
    const handleProceedToStep3 = () => {
        setSubmitError(null);
        setStep(3);
    };

    const handleFinishOrder = async () => {
        if (isSubmitting) return;

        if (paymentMethod === 'BANK_TRANSFER' && !paymentSlip) {
            setSubmitError('กรุณาอัปโหลดสลิปการโอนเงิน');
            return;
        }
        if (!user) {
            setSubmitError('ไม่พบข้อมูลผู้ใช้ กรุณาล็อกอินใหม่อีกครั้ง');
            return;
        }

        setIsSubmitting(true);
        setSubmitError(null);

        const payload = {
            paymentMethod,
            savedAddressId,
        };
        
        try {
            if (paymentMethod === 'PAYPAL') {
                const response = await createOrder(payload);
                const { approvalLink, orderId } = response.data;
                sessionStorage.setItem('pendingOrderId', orderId);
                window.location.href = approvalLink;
            } else if (paymentMethod === 'BANK_TRANSFER') {
                const createOrderResponse = await createOrder(payload);
                const orderId = createOrderResponse.data.orderId;

                if (!orderId) {
                    throw new Error("ไม่ได้รับ Order ID จากเซิร์ฟเวอร์");
                }
                
                await submitSlip(orderId, paymentSlip);
                
                await clearCart();
                navigate('/profile/orders', { state: { message: `สร้างคำสั่งซื้อ ${orderId} และส่งสลิปสำเร็จ!` } });
            }
        } catch (error) {
            console.error('Failed to process order:', error);
            const errorMessage = error.response?.data?.message || 'เกิดข้อผิดพลาดในการสร้างคำสั่งซื้อ';
            setSubmitError(errorMessage);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleFileUploadClick = () => fileInputRef.current.click();
    const handleFileChange = (e) => { if (e.target.files?.[0]) setPaymentSlip(e.target.files[0]); };
    const removePaymentSlip = () => { setPaymentSlip(null); if (fileInputRef.current) fileInputRef.current.value = null; };

    const isStep3ButtonDisabled = !savedAddressId;

    const renderStepIndicator = () => (
        <div className={styles.stepIndicator}>
            <div className={`${styles.stepButton} ${styles.firstStep} ${step >= 1 ? styles.active : ''} ${step > 1 ? styles.clickable : ''}`} onClick={() => handleStepClick(1)}>
                1. ตะกร้าสินค้า
            </div>
            <div className={`${styles.stepButton} ${step >= 2 ? styles.active : ''} ${step > 2 ? styles.clickable : ''}`} onClick={() => handleStepClick(2)}>
                2. รายละเอียด
            </div>
            <div className={`${styles.stepButton} ${styles.lastStep} ${step >= 3 ? styles.active : ''}`}>
                3. ชำระเงิน
            </div>
        </div>
    );
    
    const renderEmptyCart = () => (
        <Container className="text-center my-5">
            <Card className={styles.emptyCartCard}>
                <Card.Body>
                    <BsCartX className={styles.emptyCartIcon} />
                    <h2 className="mt-3">ตะกร้าสินค้าของคุณว่างเปล่า</h2>
                    <p className='text-muted'>ดูเหมือนจะยังไม่มีสินค้าในตะกร้าของคุณ</p>
                    <Button as={Link} to="/" variant="primary" size="lg" className="mt-3">
                        เลือกซื้อสินค้าต่อ
                    </Button>
                </Card.Body>
            </Card>
        </Container>
    );

    const renderOrderSummaryCard = (isSticky = false) => (
        <Card className={isSticky ? styles.summaryCard : ''}>
            <Card.Body>
                <Card.Title as="h5">สรุปรายการ</Card.Title>
                <hr/>
                {cartItems.map(item => (
                    <div key={item.cartItemId} className="d-flex justify-content-between small mb-2">
                        <span>{item.name} x {item.quantity}</span>
                        <span>฿{(item.lineTotal).toLocaleString()}</span>
                    </div>
                ))}
                <hr/>
                <div className="d-flex justify-content-between fw-bold fs-5">
                    <span>ยอดรวมสุทธิ</span>
                    <span>฿{totalAmount.toLocaleString()}</span>
                </div>
            </Card.Body>
        </Card>
    );

    const renderCartStep = () => (
        <Row>
            <Col lg={8}>
                <h4 className='mb-3'>ตะกร้าสินค้า ({cartItems.length} รายการ)</h4>
                {cartItems.map(item => (
                    <Card key={item.cartItemId} className={`${styles.cartCard} mb-3 ${isUpdating && updatingItemId === item.cartItemId ? styles.updating : ''}`}>
                        <Card.Body>
                            <Row className="align-items-center g-3">
                                <Col xs={3} md={2}>
                                    <div className={styles.imageContainer}>
                                        {item.itemType === 'BUILD' ? (
                                            <FaWrench className={styles.buildIcon} />
                                        ) : (
                                            <Image src={item.imageUrl || 'https://via.placeholder.com/150'} className={styles.productImage} />
                                        )}
                                    </div>
                                </Col>
                                <Col xs={9} md={10}>
                                    <Row className="align-items-center">
                                        <Col md={5}>
                                            <h6 className="mb-1">{item.name}</h6>
                                            <p className="text-muted small mb-0">฿{item.unitPrice.toLocaleString()}</p>
                                        </Col>
                                        <Col xs={7} md={3} className="mt-2 mt-md-0">
                                            <div className={styles.quantityControl}>
                                                <button className={styles.quantityButton} onClick={() => updateQuantity(item.cartItemId, item.quantity - 1)} disabled={isUpdating || item.quantity <= 1}><FaMinus size={12} /></button>
                                                <span className={styles.quantityDisplay}>{item.quantity}</span>
                                                <button className={styles.quantityButton} onClick={() => updateQuantity(item.cartItemId, item.quantity + 1)} disabled={isUpdating}><FaPlus size={12} /></button>
                                            </div>
                                        </Col>
                                        <Col xs={5} md={2} className="text-md-center mt-2 mt-md-0">
                                            <p className={`${styles.lineTotal} mb-0`}>฿{item.lineTotal.toLocaleString()}</p>
                                        </Col>
                                        <Col xs={12} md={2} className="text-md-end mt-2 mt-md-0">
                                            <Button variant="link" className="text-danger p-0" onClick={() => removeFromCart(item.cartItemId)} disabled={isUpdating}>
                                               <FaTrash /> <span className='d-inline d-md-none'>ลบ</span>
                                            </Button>
                                        </Col>
                                    </Row>
                                </Col>
                            </Row>
                            {item.itemType === 'BUILD' && item.containedItemsSnapshot && (
                                <>
                                    <hr className={styles.componentDivider} />
                                    <div className={styles.componentList}>
                                        {item.containedItemsSnapshot.map(component => (
                                            <div key={component.componentId || component.name} className={styles.componentItem}>
                                                <div className={styles.componentImageContainer}>
                                                    <Image src={component.imageUrl || 'https://via.placeholder.com/50'} className={styles.componentImage} />
                                                </div>
                                                <span className="small">{component.name}</span>
                                            </div>
                                        ))}
                                    </div>
                                </>
                            )}
                        </Card.Body>
                    </Card>
                ))}
            </Col>

            <Col lg={4}>
                {renderOrderSummaryCard(true)}
                <Button variant="primary" size="lg" className="w-100 mt-3" onClick={handleProceedToStep2} disabled={cartItems.length === 0}>
                    ดำเนินการต่อ
                </Button>
            </Col>
        </Row>
    );
  
    const renderDetailsStep = () => {
        const getAddressCardClasses = (addr) => {
            const classes = [styles.addressCard];
            if (savedAddressId === addr.id) {
                classes.push(styles.selected);
                if (addr.isDefault) {
                    classes.push(styles.defaultAndSelected);
                }
            }
            return classes.join(' ');
        };
    
        return (
            <>
                <Row>
                    <Col lg={7}>
                        <Card className="p-4">
                            <div className={styles.formSection}>
                                <h5>ช่องทางการชำระเงิน</h5>
                                <Form>
                                    <Form.Check type="radio" id="paypal" label="PayPal / บัตรเครดิต" value="PAYPAL" checked={paymentMethod === 'PAYPAL'} onChange={(e) => setPaymentMethod(e.target.value)} />
                                    <Form.Check type="radio" id="bank-transfer" label="โอนจ่ายผ่านบัญชีธนาคาร" value="BANK_TRANSFER" checked={paymentMethod === 'BANK_TRANSFER'} onChange={(e) => setPaymentMethod(e.target.value)} />
                                </Form>
                            </div>
                            <div className={styles.formSection}>
                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <h5>ที่อยู่จัดส่ง</h5>
                                    <Button variant="outline-primary" size="sm" onClick={() => handleOpenAddressModal(null)}>
                                        <FaPlusCircle className="me-2"/>เพิ่มที่อยู่ใหม่
                                    </Button>
                                </div>
                                {isAddressLoading ? (
                                    <div className="text-center p-3"><Spinner animation="border" size="sm" /></div>
                                ) : (
                                    <div className="mt-2">
                                        {userAddresses.length === 0 ? (
                                            <Alert variant="info">กรุณาเพิ่มที่อยู่สำหรับจัดส่ง</Alert>
                                        ) : (
                                            userAddresses.map(addr => (
                                                <div key={addr.id} className={getAddressCardClasses(addr)} onClick={() => setSavedAddressId(addr.id)}>
                                                    {savedAddressId === addr.id && ( <FaCheckCircle className={styles.checkIcon} /> )}
                                                    <div>
                                                        <strong>{addr.contactName}</strong>
                                                        <p className="text-muted small mb-1">{addr.phoneNumber}</p>
                                                        <p className="small mb-0">{`${addr.line1}${addr.line2 ? `, ${addr.line2}` : ''}`}</p>
                                                        <p className="small mb-0">{`${addr.subdistrict}, ${addr.district}, ${addr.province} ${addr.zipCode}`}</p>
                                                        <Button variant="link" size="sm" className="p-0 mt-1" onClick={(e) => { e.stopPropagation(); handleOpenAddressModal(addr); }}>
                                                            <FaEdit className="me-1" /> แก้ไข
                                                        </Button>
                                                    </div>
                                                </div>
                                            ))
                                        )}
                                    </div>
                                )}
                            </div>
                        </Card>
                    </Col>
                    <Col lg={5}>
                        {renderOrderSummaryCard()}
                        {submitError && <Alert variant="danger" className="mt-3">{submitError}</Alert>}
                        <OverlayTrigger
                            placement="top"
                            overlay={isStep3ButtonDisabled ? ( <Tooltip id="tooltip-disabled">กรุณาเลือกที่อยู่สำหรับจัดส่ง</Tooltip> ) : ( <span /> )}
                        >
                            <span className="d-grid w-100 mt-3">
                                <Button
                                    variant="primary"
                                    size="lg"
                                    onClick={handleProceedToStep3}
                                    disabled={isStep3ButtonDisabled}
                                    style={isStep3ButtonDisabled ? { pointerEvents: 'none' } : {}}
                                >
                                    ดำเนินการต่อ
                                </Button>
                            </span>
                        </OverlayTrigger>
                    </Col>
                </Row>
                <AddressModal
                    show={showAddressModal}
                    handleClose={() => setShowAddressModal(false)}
                    handleSave={handleSaveAddress}
                    address={editingAddress}
                />
            </>
        );
    };

    const renderPaymentStep = () => (
        <Row>
            {paymentMethod === 'BANK_TRANSFER' ? (
                <Col lg={7}>
                    <Card className="p-4">
                        {isBankDetailsLoading ? (
                            <div className="text-center p-5"><Spinner animation="border" /></div>
                        ) : !bankDetails ? (
                            <Alert variant="warning">ไม่สามารถโหลดข้อมูลการชำระเงินได้ในขณะนี้</Alert>
                        ) : (
                            <div>
                                <div className={styles.paymentSlipContainer}>
                                    <div className={styles.slipHeader}>THAI QR PAYMENT</div>
                                    <div className={styles.slipBody}>
                                        <div className={styles.promptPayText}>PromptPay</div>
                                        <Image src={bankDetails.qrCodeImageUrl} fluid className={styles.qrImage}/>
                                        <div className={styles.slipDetails}>
                                            <p><span>Account Name:</span> <strong>{bankDetails.accountName}</strong></p>
                                            <p><span>Bank:</span> <strong>{bankDetails.bankName}</strong></p>
                                            <p><span>Account No:</span> <strong>{bankDetails.accountNumber}</strong></p>
                                            <hr />
                                            <p className="fs-6"><span>Amount:</span> <strong className="fs-5">฿{totalAmount.toLocaleString()}</strong></p>
                                        </div>
                                    </div>
                                </div>
                                <div>
                                    <p className="fw-bold mt-4">แจ้งการชำระเงิน</p>
                                    <p className='text-muted small'>กรุณาอัปโหลดสลิปเพื่อยืนยันการชำระเงิน</p>
                                    <input type="file" ref={fileInputRef} onChange={handleFileChange} className={styles.hiddenInput} accept="image/*,.pdf" />
                                    <Button variant="outline-primary" onClick={handleFileUploadClick}><FaUpload className="me-2" /> อัปโหลดสลิป</Button>
                                    {paymentSlip && (
                                        <div className="mt-2 d-flex align-items-center">
                                            <span className="me-2 text-success">{paymentSlip.name}</span>
                                            <Button variant="link" className="text-danger p-0" onClick={removePaymentSlip}><FaTrash /></Button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </Card>
                </Col>
            ) : (
                <Col lg={7}>
                    <Card className="p-4 text-center">
                        <h5 className="mb-3">ดำเนินการต่อด้วย PayPal</h5>
                        <p>คุณจะถูกส่งไปยังหน้าเว็บไซต์ของ PayPal เพื่อทำการชำระเงินให้เสร็จสิ้น</p>
                        <Image src="https://www.paypalobjects.com/webstatic/mktg/logo/AM_SbyPP_mc_vs_dc_ae.jpg" fluid style={{maxWidth: '300px'}} />
                    </Card>
                </Col>
            )}
            <Col lg={5}>
                {renderOrderSummaryCard()}
                {submitError && <Alert variant="danger" className="mt-3">{submitError}</Alert>}
                <Button variant="success" size="lg" className="w-100 mt-3" onClick={handleFinishOrder} disabled={isSubmitting}>
                    {isSubmitting ? (<><Spinner as="span" animation="border" size="sm" /> กำลังดำเนินการ...</>) : 
                     (paymentMethod === 'PAYPAL' ? 'ไปยังหน้าชำระเงิน PayPal' : 'ยืนยันการสั่งซื้อ')
                    }
                </Button>
            </Col>
        </Row>
    );

    if (isAuthLoading || isCartLoading) {
        return <Container className="d-flex justify-content-center align-items-center" style={{ minHeight: '60vh' }}><Spinner animation="border" /></Container>;
    }
    
    return (
        <Container className={styles.pageContainer} fluid>
            {renderStepIndicator()}
            {cartItems.length === 0 && step === 1 ? renderEmptyCart() : (
                <>
                    {step === 1 && renderCartStep()}
                    {step === 2 && renderDetailsStep()}
                    {step === 3 && renderPaymentStep()}
                </>
            )}
        </Container>
    );
};

export default CartPage;