import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Image, Button, Spinner, Alert, Form, Breadcrumb, ListGroup } from 'react-bootstrap';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../../api/api';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { notifySuccess } from '../../services/NotificationService';
import { getCategoryNameBySlug } from '../../component/Product/categories';


const arrayFormatter = (arr) => arr && arr.length > 0 ? arr.join(', ') : 'N/A';
const SPEC_CONFIG = {
    case: [
        { key: 'mpn', label: 'MPN' },
        { key: 'supportedFormFactors', label: 'Supported Form Factors', formatter: arrayFormatter },
        { key: 'supportedPsuFormFactors', label: 'Supported PSU Form Factors', formatter: arrayFormatter },
        { key: 'max_gpu_length_mm', label: 'Max GPU Length', unit: ' mm' },
        { key: 'max_cooler_height_mm', label: 'Max CPU Cooler Height', unit: ' mm' },
        { key: 'bays_2_5_inch', label: '2.5" Bays' },
        { key: 'bays_3_5_inch', label: '3.5" Bays' },
        { key: 'supportedRadiatorSizesMm', label: 'Supported Radiator Sizes (mm)', formatter: arrayFormatter }
    ],
    cooler: [
        { key: 'mpn', label: 'MPN' },
        { key: 'wattage', label: 'Recommended Wattage', unit: ' W' },
        { key: 'supportedSockets', label: 'Supported Sockets', formatter: arrayFormatter },
        { key: 'height_mm', label: 'Height', unit: ' mm' },
        { key: 'radiatorSize_mm', label: 'Radiator Size', unit: ' mm' }
    ],
    cpu: [
        { key: 'mpn', label: 'MPN' },
        { key: 'wattage', label: 'TDP', unit: ' W' },
        { key: 'socket', label: 'Socket', formatter: (socketObj) => socketObj?.name || 'N/A' }
    ],
    
};

const ProductDetailPage = () => {
    const { productId } = useParams();
    const navigate = useNavigate();
    
    const { user } = useAuth();
    const { addToCart, isUpdating } = useCart();

    const [product, setProduct] = useState(null);
    const [quantity, setQuantity] = useState(1);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchProduct = async () => {
            setLoading(true);
            setError(null);
            try {
                const response = await api.get(`/components/${productId}`);
                setProduct(response.data);
            } catch (err) {
                console.error("Failed to fetch product details:", err);
                setError("ไม่พบสินค้าที่คุณกำลังค้นหา หรือเกิดข้อผิดพลาด");
            } finally {
                setLoading(false);
            }
        };
        fetchProduct();
    }, [productId]);

    
    const handleAddToCart = async () => {
        if (!user) {
            alert('กรุณาเข้าสู่ระบบก่อนเพิ่มสินค้าลงตะกร้า');
            navigate('/login');
            return;
        }

        if (product && quantity > 0) {
            const itemData = {
                productId: product.id || product._id,
                quantity: quantity,
                itemType: 'COMPONENT'
            };

            try {
                await addToCart(itemData);
                notifySuccess(`เพิ่ม '${product.name}' จำนวน ${quantity} ชิ้น ลงในตะกร้าแล้ว`);
            } catch (err) {
                console.error("Failed to add to cart from detail page:", err);
                
            }
        }
    };

  
    if (loading) {
        return <Container className="text-center my-5"><Spinner animation="border" /></Container>;
    }

    if (error || !product) {
        return <Container className="my-5"><Alert variant="danger">{error || "ไม่พบข้อมูลสินค้า"}</Alert></Container>;
    }
    
    const productSpecsConfig = SPEC_CONFIG[product.type] || [];

    return (
        <Container className="my-5">
            <Breadcrumb>
                <Breadcrumb.Item onClick={() => navigate('/')} style={{ cursor: 'pointer', color: '#0d6efd' }}>
                    หน้าแรก
                </Breadcrumb.Item>
                <Breadcrumb.Item as={Link} to={`/products/category/${product.type}`}>
                    {getCategoryNameBySlug(product.type) || product.type}
                </Breadcrumb.Item>
                <Breadcrumb.Item active>{product.name}</Breadcrumb.Item>
            </Breadcrumb>
            
            <Row className="mt-4">
                <Col md={6} className="mb-4 mb-md-0">
                    <Image src={product.imageUrl || 'https://via.placeholder.com/500'} fluid rounded />
                </Col>
                <Col md={6}>
                    <Card className="border-0">
                        <Card.Body>
                            <Card.Title as="h2">{product.name}</Card.Title>
                            <Card.Text className="text-muted">{product.description}</Card.Text>
                            <div className="my-4">
                                <span className="fs-2 fw-bold text-danger">
                                    {product.price ? `฿${product.price.toLocaleString()}` : 'ติดต่อเพื่อสอบถามราคา'}
                                </span>
                            </div>
                            
                            <Row className="align-items-center">
                                <Col xs="auto">
                                    <Form.Label htmlFor="quantity-input" className="mb-0">จำนวน:</Form.Label>
                                </Col>
                                <Col xs={4} sm={3}>
                                    <Form.Control 
                                        id="quantity-input"
                                        type="number" 
                                        value={quantity}
                                        onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                                        min="1"
                                    />
                                </Col>
                            </Row>

                            <Button 
                                variant="danger" 
                                size="lg" 
                                className="mt-4 w-100"
                                onClick={handleAddToCart}
                                disabled={isUpdating}
                            >
                                {isUpdating ? 'กำลังเพิ่ม...' : 'เพิ่มลงตะกร้า'}
                            </Button>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {productSpecsConfig.length > 0 && (
                <Row className="mt-5">
                    <Col>
                        <Card>
                            <Card.Header as="h5">Specifications</Card.Header>
                            <ListGroup variant="flush">
                                {productSpecsConfig.map(spec => {
                                    const value = product[spec.key];
                                    if (value === null || value === undefined || value === '') return null;

                                    const displayValue = spec.formatter ? spec.formatter(value) : value;
                                    const unit = spec.unit || '';

                                    return (
                                        <ListGroup.Item key={spec.key} className="d-flex justify-content-between">
                                            <strong>{spec.label}</strong>
                                            <span>{displayValue}{unit}</span>
                                        </ListGroup.Item>
                                    );
                                })}
                            </ListGroup>
                        </Card>
                    </Col>
                </Row>
            )}
        </Container>
    );
};

export default ProductDetailPage;