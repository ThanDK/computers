import React, { useState, useEffect, useMemo } from 'react';
import { Container, Row, Col, Card, Button, Spinner, Alert, ListGroup, ButtonGroup, Form, InputGroup } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { FaPlus, FaWrench, FaTrash, FaShoppingCart, FaMicrochip, FaVideo, FaTh, FaList, FaSearch } from 'react-icons/fa';
import api from '../../api/api';
import { useCart } from '../../context/CartContext';
import { useBuild } from '../../context/BuildContext';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';
import styles from './MyBuilds.module.css';

const MyBuilds = () => {
    const [builds, setBuilds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [viewMode, setViewMode] = useState('grid');
    const [searchQuery, setSearchQuery] = useState('');
    const [sortOrder, setSortOrder] = useState('name-asc');
    const navigate = useNavigate();

    const { addToCart, updatingProductId } = useCart();
    const { clearBuild } = useBuild();

    useEffect(() => {
        const fetchBuilds = async () => {
            try {
                setLoading(true);
                const response = await api.get('/builds');
                setBuilds(response.data);
                setError(null);
            } catch (err) {
                if (err.response && err.response.status === 404) {
                    setBuilds([]);
                    setError(null);
                } else {
                    const errorMessage = 'Failed to load your saved builds. Please try again later.';
                    setError(errorMessage);
                    notifyError(errorMessage);
                    console.error("An error occurred while fetching builds:", err);
                }
            } finally {
                setLoading(false);
            }
        };
        fetchBuilds();
    }, []);
    
    const displayedBuilds = useMemo(() => {
        let filteredBuilds = [...builds];

        if (searchQuery) {
            filteredBuilds = filteredBuilds.filter(build =>
                build.buildName.toLowerCase().includes(searchQuery.toLowerCase())
            );
        }

        switch (sortOrder) {
            case 'name-asc':
                filteredBuilds.sort((a, b) => a.buildName.localeCompare(b.buildName));
                break;
            case 'name-desc':
                filteredBuilds.sort((a, b) => b.buildName.localeCompare(a.buildName));
                break;
            case 'price-desc':
                filteredBuilds.sort((a, b) => (b.totalPrice || 0) - (a.totalPrice || 0));
                break;
            case 'price-asc':
                filteredBuilds.sort((a, b) => (a.totalPrice || 0) - (b.totalPrice || 0));
                break;
            default:
                break;
        }

        return filteredBuilds;
    }, [builds, searchQuery, sortOrder]);

    const handleDelete = async (buildId, buildName) => {
        const isConfirmed = await showConfirmation(
            'ยืนยันการลบ',
            `คุณแน่ใจหรือไม่ว่าต้องการลบ Build "${buildName}"? การกระทำนี้ไม่สามารถย้อนกลับได้`
        );

        if (isConfirmed) {
            try {
                await api.delete(`/builds/${buildId}`);
                setBuilds(currentBuilds => currentBuilds.filter(b => b.id !== buildId));
                notifySuccess(`ลบ Build "${buildName}" สำเร็จแล้ว`);
            } catch (err) {
                notifyError('เกิดข้อผิดพลาดในการลบ Build');
                console.error(err);
            }
        }
    };

    const handleAddToCart = async (build) => {
        try {
            const itemData = {
                productId: build.id,
                itemType: 'BUILD',
                quantity: 1
            };
            await addToCart(itemData);
            notifySuccess(`เพิ่ม "${build.buildName}" ลงในตะกร้าแล้ว!`);
            navigate('/cart');
        } catch (error) {
            notifyError('เกิดข้อผิดพลาดในการเพิ่ม Build ลงตะกร้า');
            console.error("Add to cart failed from MyBuilds page:", error);
        }
    };

    const handleCreateNewBuild = () => {
        clearBuild();
        navigate('/build/new');
    };

    const renderEmptyState = () => (
        <Col xs={12}>
            <div className={styles.emptyState}>
                <h3>ยังไม่มี Build ที่บันทึกไว้</h3>
                <p>เริ่มต้นจัดสเปคคอมพิวเตอร์ในฝันของคุณได้เลย</p>
                <Button onClick={handleCreateNewBuild} variant="success" size="lg">
                    <FaPlus className="me-2" /> สร้าง Build ใหม่
                </Button>
            </div>
        </Col>
    );
    
    const renderNoResultsState = () => (
        <Col xs={12}>
             <Alert variant="info" className="text-center mt-4">
                <h4><FaSearch className="me-2" /> ไม่พบผลลัพธ์</h4>
                <p className="mb-0">ไม่พบ Build ที่ตรงกับการค้นหาของคุณ ลองใช้คำค้นหาอื่น</p>
            </Alert>
        </Col>
    );

    const renderBuildList = () => {
        return displayedBuilds.map(build => (
            <Col xs={12} key={build.id} className="mb-3">
                <Card className={styles.buildCardList}>
                    <Card.Body>
                        <Row className="align-items-center">
                            <Col md={4} className="mb-3 mb-md-0">
                                <h5 className="fw-bold mb-1">{build.buildName}</h5>
                                <span className={styles.totalPrice}>
                                    Total: ฿{build.totalPrice?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || 'N/A'}
                                </span>
                            </Col>
                            <Col md={5}>
                                <div className={styles.specListHorizontal}>
                                    <span className={styles.specItem}>
                                        <FaMicrochip className={styles.specIcon} /> {build.cpu?.name || 'N/A'}
                                    </span>
                                    <span className={styles.specItem}>
                                        <FaVideo className={styles.specIcon} /> {build.gpus?.[0]?.partDetails?.name || 'N/A'}
                                    </span>
                                </div>
                            </Col>
                            <Col md={3} className={styles.actionButtons}>
                                <Button variant="primary" size="sm" onClick={() => navigate(`/build/${build.id}`)}>
                                    <FaWrench className="me-1" /> Edit
                                </Button>
                                <Button
                                    variant="success"
                                    size="sm"
                                    onClick={() => handleAddToCart(build)}
                                    disabled={!!updatingProductId}
                                >
                                    {updatingProductId === build.id ? (
                                        <Spinner as="span" animation="border" size="sm" />
                                    ) : (
                                        <FaShoppingCart />
                                    )}
                                    <span className="ms-1">Add</span>
                                </Button>
                                <Button variant="outline-danger" size="sm" onClick={() => handleDelete(build.id, build.buildName)}>
                                    <FaTrash />
                                </Button>
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
            </Col>
        ));
    };

    const renderBuildGrid = () => {
        return displayedBuilds.map(build => (
            <Col md={6} lg={4} key={build.id} className="mb-4">
                <Card className={`${styles.buildCard} h-100`}>
                    <Card.Body className="d-flex flex-column">
                        <Card.Title className="fw-bold">{build.buildName}</Card.Title>
                        <Card.Subtitle className="mb-3 text-muted">
                            Total: ฿{build.totalPrice?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || 'N/A'}
                        </Card.Subtitle>
                        <ListGroup variant="flush" className={`${styles.specList} flex-grow-1`}>
                            <ListGroup.Item>
                                <FaMicrochip className={styles.specIcon} /> {build.cpu?.name || 'N/A'}
                            </ListGroup.Item>
                            <ListGroup.Item>
                                <FaVideo className={styles.specIcon} /> {build.gpus?.[0]?.partDetails?.name || 'N/A'}
                            </ListGroup.Item>
                        </ListGroup>
                        <div className={`${styles.actionButtonsGrid} mt-auto pt-3`}>
                            <Button variant="primary" size="sm" onClick={() => navigate(`/build/${build.id}`)}>
                                <FaWrench className="me-1" /> Edit
                            </Button>
                            <Button
                                variant="success"
                                size="sm"
                                onClick={() => handleAddToCart(build)}
                                disabled={!!updatingProductId}
                            >
                                {updatingProductId === build.id ? (
                                    <Spinner as="span" animation="border" size="sm" />
                                ) : (
                                    <FaShoppingCart className="me-1" />
                                )}
                                Add
                            </Button>
                            <Button variant="outline-danger" size="sm" onClick={() => handleDelete(build.id, build.buildName)}>
                                <FaTrash />
                            </Button>
                        </div>
                    </Card.Body>
                </Card>
            </Col>
        ));
    };

    return (
        <Container className="py-4">
            <Row className="mb-4 align-items-center">
                <Col className="d-flex align-items-center">
                    <h1 className="mb-0 me-3">My Computer Builds</h1>
                    {builds.length > 0 && (
                         <ButtonGroup>
                            <Button variant={viewMode === 'grid' ? 'primary' : 'outline-secondary'} onClick={() => setViewMode('grid')}>
                                <FaTh />
                            </Button>
                            <Button variant={viewMode === 'list' ? 'primary' : 'outline-secondary'} onClick={() => setViewMode('list')}>
                                <FaList />
                            </Button>
                        </ButtonGroup>
                    )}
                </Col>
                <Col xs="auto">
                    <Button onClick={handleCreateNewBuild} variant="success" size="lg">
                        <FaPlus className="me-2" /> สร้าง Build ใหม่
                    </Button>
                </Col>
            </Row>

            {builds.length > 0 && (
                <Row className="mb-4 p-3 bg-light border rounded align-items-end">
                    <Col md={5}>
                        <Form.Group>
                            <Form.Label>ค้นหาตามชื่อ Build</Form.Label>
                            <InputGroup>
                                <InputGroup.Text><FaSearch /></InputGroup.Text>
                                <Form.Control
                                    type="text"
                                    placeholder="เช่น Gaming PC..."
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                />
                            </InputGroup>
                        </Form.Group>
                    </Col>
                    <Col md={3}>
                        <Form.Group>
                            <Form.Label>จัดเรียงตาม</Form.Label>
                            <Form.Select value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}>
                                <option value="name-asc">ชื่อ (A-Z)</option>
                                <option value="name-desc">ชื่อ (Z-A)</option>
                                <option value="price-desc">ราคา (มากไปน้อย)</option>
                                <option value="price-asc">ราคา (น้อยไปมาก)</option>
                            </Form.Select>
                        </Form.Group>
                    </Col>
                </Row>
            )}

            <Row>
                {loading ? (
                    <div className="text-center p-5"><Spinner animation="border" /> <p className="mt-2">Loading your builds...</p></div>
                ) : error ? (
                    <Alert variant="danger">{error}</Alert>
                ) : builds.length === 0 ? (
                    renderEmptyState()
                ) : displayedBuilds.length === 0 ? (
                    renderNoResultsState()
                ) : (
                    viewMode === 'grid' ? renderBuildGrid() : renderBuildList()
                )}
            </Row>
        </Container>
    );
};

export default MyBuilds;