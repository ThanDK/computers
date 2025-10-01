import React, { useState, useRef, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Image, Form, Spinner, Alert, Badge } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { createOrder, submitSlip, getUserAddresses } from '../../services/OrderService';
import NewAddressForm from '../../component/Address/NewAddressForm';
// * 1. เพิ่มไอคอน FaWrench สำหรับใช้กับสินค้าประเภท Build
import { FaPlus, FaMinus, FaTrash, FaUpload, FaCheckCircle, FaWrench } from 'react-icons/fa';
import { BsCartX } from 'react-icons/bs'; // ไอคอนสำหรับตะกร้าว่าง
import styles from './CartPage.module.css';

const initialAddressState = {
    contactName: '', phoneNumber: '', line1: '', line2: '',
    subdistrict: '', district: '', province: '', zipCode: '', country: 'Thailand'
};

const CartPage = () => {
    // ... (โค้ดส่วนอื่น ๆ เหมือนเดิมทั้งหมด) ...
    const { cartItems, removeFromCart, updateQuantity, totalAmount, clearCart, isLoading: isCartLoading, isUpdating, updatingItemId } = useCart();
    const { user, token, isLoading: isAuthLoading } = useAuth();
    const navigate = useNavigate();

    const [step, setStep] = useState(1);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState(null);
    const [addressSelection, setAddressSelection] = useState('saved');
    const [savedAddressId, setSavedAddressId] = useState('');
    const [newAddress, setNewAddress] = useState(initialAddressState);
    const [userAddresses, setUserAddresses] = useState([]);
    const [isAddressLoading, setIsAddressLoading] = useState(true);
    const [paymentMethod, setPaymentMethod] = useState('PAYPAL');
    const [paymentSlip, setPaymentSlip] = useState(null);
    const fileInputRef = useRef(null);

    // ... (useEffect และฟังก์ชัน handle ต่าง ๆ เหมือนเดิมทั้งหมด) ...
    useEffect(() => {
        const fetchAddresses = async () => {
            if (user && token) {
                try {
                    setIsAddressLoading(true);
                    const response = await getUserAddresses();
                    const addresses = response.data;
                    setUserAddresses(addresses);
                    const defaultAddress = addresses.find(addr => addr.isDefault);
                    if (defaultAddress) {
                        setSavedAddressId(defaultAddress.id);
                        setAddressSelection('saved');
                    } else if (addresses.length > 0) {
                        setSavedAddressId(addresses[0].id);
                        setAddressSelection('saved');
                    } else {
                        setAddressSelection('new');
                    }
                } catch (error) {
                    console.error("Failed to fetch addresses:", error);
                    setAddressSelection('new');
                } finally {
                    setIsAddressLoading(false);
                }
            } else {
                setAddressSelection('new');
                setIsAddressLoading(false);
            }
        };

        if (step === 2 && !isAuthLoading) {
            fetchAddresses();
        }
    }, [step, user, token, isAuthLoading]);

    const handleProceedToStep2 = () => setStep(2);
    const handleProceedToStep3 = () => {
        if (!user) {
            alert('กรุณาเข้าสู่ระบบก่อนดำเนินการต่อ');
            navigate('/login');
            return;
        }
        if (addressSelection === 'saved' && !savedAddressId) {
            setSubmitError('กรุณาเลือกที่อยู่สำหรับจัดส่ง');
            return;
        }
        if (addressSelection === 'new' && (!newAddress.contactName || !newAddress.line1 || !newAddress.province || !newAddress.zipCode)) {
            setSubmitError('กรุณากรอกข้อมูลที่อยู่ใหม่ให้ครบถ้วน');
            return;
        }
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
            ...(addressSelection === 'saved' ? { savedAddressId } : { newAddress }),
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
                
                alert(`สร้างคำสั่งซื้อ ${orderId} และส่งสลิปสำเร็จ!`);
                await clearCart();
                navigate('/profile/orders');
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
    const handleFileChange = (e) => {
        if (e.target.files?.[0]) setPaymentSlip(e.target.files[0]);
    };
    const removePaymentSlip = () => {
        setPaymentSlip(null);
        if (fileInputRef.current) fileInputRef.current.value = null;
    };

    // --- RENDER FUNCTIONS ---
    // ... (renderStepIndicator, renderEmptyCart, renderOrderSummaryCard เหมือนเดิม) ...
    const renderStepIndicator = () => (
        <div className={styles.stepIndicator}>
            <div className={`${styles.stepButton} ${styles.firstStep} ${step >= 1 ? styles.active : ''}`}>1. ตะกร้าสินค้า</div>
            <div className={`${styles.stepButton} ${step >= 2 ? styles.active : ''}`}>2. รายละเอียด</div>
            <div className={`${styles.stepButton} ${styles.lastStep} ${step >= 3 ? styles.active : ''}`}>3. ชำระเงิน</div>
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
                                        {/* // * 2. เพิ่มเงื่อนไขในการแสดงผลรูปภาพหรือไอคอน */}
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
  
    // ... (โค้ดส่วน renderDetailsStep, renderPaymentStep และส่วน return เหมือนเดิมทั้งหมด) ...
    const renderDetailsStep = () => (
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
                        <h5>ที่อยู่จัดส่ง</h5>
                        {isAddressLoading ? (
                            <div className="text-center p-3"><Spinner animation="border" size="sm" /> กำลังโหลดข้อมูลที่อยู่...</div>
                        ) : (
                            <Form>
                                {userAddresses.length > 0 && user && (
                                    <Form.Check type="radio" id="saved-address" label="เลือกจากที่อยู่ที่บันทึกไว้" value="saved" checked={addressSelection === 'saved'} onChange={(e) => setAddressSelection(e.target.value)} />
                                )}
                                {addressSelection === 'saved' && userAddresses.length > 0 && (
                                    <div className="mt-2">
                                        {userAddresses.map(addr => (
                                            <div key={addr.id} className={`${styles.addressCard} ${savedAddressId === addr.id ? styles.selected : ''}`} onClick={() => setSavedAddressId(addr.id)}>
                                                {savedAddressId === addr.id && ( <FaCheckCircle className={styles.checkIcon} /> )}
                                                <div className="d-flex justify-content-between align-items-start">
                                                    <strong>{addr.contactName}</strong>
                                                    {addr.isDefault && <Badge bg="success">ที่อยู่หลัก</Badge>}
                                                </div>
                                                <p className="text-muted small mb-1">{addr.phoneNumber}</p>
                                                <p className="small mb-0">{`${addr.line1}${addr.line2 ? `, ${addr.line2}` : ''}`}</p>
                                                <p className="small mb-0">{`${addr.subdistrict}, ${addr.district}, ${addr.province} ${addr.zipCode}`}</p>
                                            </div>
                                        ))}
                                    </div>
                                )}
                                <Form.Check type="radio" id="new-address" label="เพิ่มที่อยู่ใหม่" value="new" checked={addressSelection === 'new'} onChange={(e) => setAddressSelection(e.target.value)} className={userAddresses.length > 0 ? 'mt-3' : ''}/>
                                {addressSelection === 'new' && (
                                    <NewAddressForm address={newAddress} onChange={setNewAddress} />
                                )}
                            </Form>
                        )}
                    </div>
                </Card>
            </Col>
            <Col lg={5}>
                {renderOrderSummaryCard()}
                {submitError && <Alert variant="danger" className="mt-3">{submitError}</Alert>}
                <Button variant="primary" size="lg" className="w-100 mt-3" onClick={handleProceedToStep3}>ดำเนินการต่อ</Button>
            </Col>
        </Row>
    );

    const renderPaymentStep = () => (
         <Row>
            {paymentMethod === 'BANK_TRANSFER' ? (
                <Col lg={7}>
                     <Card className="p-4">
                        <h5>โอนจ่ายผ่านบัญชี</h5>
                        <p>กรุณาชำระเงินและอัปโหลดสลิปภายใน 24 ชั่วโมง</p>
                         <Row>
                            <Col md={6} className="text-center">
                                <p className="fw-bold">Thai QR Payment</p>
                                <Image src={`https://promptpay.io/0812345678/${totalAmount}.png`} fluid className={styles.qrImage}/>
                                <p>สแกนเพื่อจ่าย</p>
                            </Col>
                            <Col md={6}>
                                <p className="fw-bold">โอนเงินเข้าบัญชี</p>
                                <Image src="https://upload.wikimedia.org/wikipedia/commons/thumb/7/7a/K-Bank_logo.svg/2560px-K-Bank_logo.svg.png" fluid className={styles.bankLogo}/>
                                <p className="mb-1 small">ธนาคาร: กสิกรไทย</p>
                                <p className="mb-1 small">ชื่อบัญชี: IT SHOP</p>
                                <p className="mb-1 small">เลขที่บัญชี: 123-4-56789-0</p>
                                <hr/>
                                <p className="mb-1 small fw-bold">ยอดที่ต้องชำระ: ฿{totalAmount.toLocaleString()}</p>
                            </Col>
                        </Row>
                        <hr />
                        <div>
                            <p className="fw-bold">แจ้งการชำระเงิน</p>
                            <input type="file" ref={fileInputRef} onChange={handleFileChange} className={styles.hiddenInput} accept="image/*,.pdf" />
                            <Button variant="outline-primary" onClick={handleFileUploadClick}><FaUpload className="me-2" /> อัปโหลดสลิป</Button>
                            {paymentSlip && (
                                <div className="mt-2 d-flex align-items-center">
                                    <span className="me-2 text-success">{paymentSlip.name}</span>
                                    <Button variant="link" className="text-danger p-0" onClick={removePaymentSlip}><FaTrash /></Button>
                                </div>
                            )}
                        </div>
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