import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Image, Button, Spinner, Alert, Form, Breadcrumb, ListGroup } from 'react-bootstrap';
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom';
import api from '../../api/api';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { useBuild } from '../../context/BuildContext';
import { notifySuccess } from '../../services/NotificationService';
import { getCategoryNameBySlug } from '../../component/Product/categories';
import { componentCategories } from '../../config/componentCategories';
import { FaPlus } from 'react-icons/fa';
import ProductSpecifications from '../../component/Product/ProductSpecifications';

const ProductDetailPage = () => {
    const { productId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    
    const { user, loading: authIsLoading } = useAuth(); // Renamed for clarity
    const { addToCart, updatingProductId } = useCart();
    const { addComponentToBuild } = useBuild();

    const [product, setProduct] = useState(null);
    const [quantity, setQuantity] = useState(1);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const queryParams = new URLSearchParams(location.search);
    const isBuilderMode = queryParams.get('source') === 'builder';
    const buildIdFromUrl = queryParams.get('buildId');

    // CORRECTED: useEffect logic is now more robust to prevent race conditions.
    useEffect(() => {
        // First, wait for the authentication process to complete.
        if (authIsLoading) {
            return;
        }

        // After auth is resolved, if there is no user, redirect to login.
        if (!user) {
            alert('กรุณาเข้าสู่ระบบเพื่อดูรายละเอียดสินค้า');
            navigate('/login', { replace: true });
            return;
        }

        // If a user exists, proceed to fetch the product data.
        const fetchProduct = async () => {
            // Only set loading to true when we are actually about to fetch.
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

    }, [authIsLoading, user, productId, navigate]);
    
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

    const handleAddToBuild = () => {
        if (product) {
            const category = componentCategories.find(c => c.dbType.toLowerCase() === product.type.toLowerCase());
            if (category) {
                addComponentToBuild(category, product);
                notifySuccess(`เพิ่ม '${product.name}' ลงใน Build ของคุณแล้ว`);
                
                if (buildIdFromUrl) {
                    navigate(`/build/${buildIdFromUrl}`);
                } else {
                    navigate('/build/new');
                }
                
            } else {
                console.error(`Could not find a matching category for product type: ${product.type}`);
            }
        }
    };

    if (authIsLoading || loading) {
        return <Container className="text-center my-5"><Spinner animation="border" /></Container>;
    }

    if (error || !product) {
        return (
            <Container className="my-5">
                <Alert variant="danger">{error || "ไม่สามารถโหลดข้อมูลสินค้าได้"}</Alert>
            </Container>
        );
    }
    
    const renderActionButton = () => {
        if (isBuilderMode) {
            return (
                <Button 
                    variant="primary"
                    size="lg" 
                    className="mt-4 w-100"
                    onClick={handleAddToBuild}
                >
                    <FaPlus className="me-2" />
                    เพิ่มลงใน Build
                </Button>
            );
        }
        return (
            <Button 
                variant="danger" 
                size="lg" 
                className="mt-4 w-100"
                onClick={handleAddToCart}
                disabled={!!updatingProductId}
            >
                {updatingProductId === product.id ? 'กำลังเพิ่ม...' : 'เพิ่มลงตะกร้า'}
            </Button>
        );
    };

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
                            
                            {!isBuilderMode && (
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
                            )}

                            {renderActionButton()}
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            <Row className="mt-5">
                <Col>
                    <ProductSpecifications product={product} />
                </Col>
            </Row>
        </Container>
    );
};

export default ProductDetailPage;