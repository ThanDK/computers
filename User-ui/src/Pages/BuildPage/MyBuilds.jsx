import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Spinner, Alert } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { FaPlus, FaWrench, FaTrash, FaShoppingCart } from 'react-icons/fa';
import api from '../../api/api';
import { useCart } from '../../context/CartContext';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';

const MyBuilds = () => {
    const [builds, setBuilds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    
    const { addToCart, isUpdating } = useCart();

    useEffect(() => {
        const fetchBuilds = async () => {
            try {
                setLoading(true);
                
                const response = await api.get('/builds');
                setBuilds(response.data);
                setError(null);
            } catch (err) {
               
                if (err.response && err.response.status === 404) {
                    console.warn("API endpoint '/builds' not found (404), which is unexpected. Displaying an empty list.");
                    setBuilds([]); 
                    setError(null); 
                } else {
                    const errorMessage = 'Failed to load your saved builds. Please try again later.';
                    setError(errorMessage);
                    notifyError(errorMessage); // Use notification for visible error
                    console.error("An error occurred while fetching builds:", err);
                }
            } finally {
                setLoading(false);
            }
        };
        fetchBuilds();
    }, []);

    const handleDelete = async (buildId, buildName) => {
        const isConfirmed = await showConfirmation(
            'Confirm Deletion',
            `Are you sure you want to delete the build "${buildName}"? This action cannot be undone.`
        );

        if (isConfirmed) {
            try {
                await api.delete(`/builds/${buildId}`);
                setBuilds(currentBuilds => currentBuilds.filter(b => b.id !== buildId));
                notifySuccess(`Build "${buildName}" has been deleted.`);
            } catch (err) {
                notifyError('Failed to delete the build.');
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
            notifySuccess(`"${build.buildName}" has been added to your cart!`);
            navigate('/cart');
        } catch (error) {
            notifyError('There was an issue adding the build to your cart.');
            console.error("Add to cart failed from MyBuilds page:", error);
        }
    };

    const renderBuilds = () => {
        if (loading) {
            return <div className="text-center p-5"><Spinner animation="border" /> <p className="mt-2">Loading your builds...</p></div>;
        }
        if (error) {
            return <Alert variant="danger">{error}</Alert>;
        }
        if (builds.length === 0) {
            return <Alert variant="info">You have no saved builds yet. Start creating one now!</Alert>;
        }
        return builds.map(build => (
            <Col md={6} lg={4} key={build.id} className="mb-4">
                <Card className="h-100 shadow-sm">
                    <Card.Body className="d-flex flex-column">
                        <Card.Title className="fw-bold">{build.buildName}</Card.Title>
                        <Card.Subtitle className="mb-3 text-muted">
                            Total: ฿{build.totalPrice?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || 'N/A'}
                        </Card.Subtitle>
                        <div className="flex-grow-1">
                            <p className="mb-1"><small><strong>CPU:</strong> {build.cpu?.name || 'N/A'}</small></p>
                            <p><small><strong>GPU:</strong> {build.gpus?.[0]?.component?.name || 'N/A'}</small></p>
                        </div>
                        <div className="mt-auto pt-3 d-flex flex-wrap gap-2 justify-content-start">
                            <Button variant="primary" size="sm" onClick={() => navigate(`/build/${build.id}`)}>
                                <FaWrench className="me-1" /> Edit
                            </Button>
                            <Button
                                variant="success"
                                size="sm"
                                onClick={() => handleAddToCart(build)}
                                
                                disabled={isUpdating}
                            >
                                {isUpdating ? (
                                    <Spinner as="span" animation="border" size="sm" />
                                ) : (
                                    <FaShoppingCart className="me-1" />
                                )}
                                Add to Cart
                            </Button>
                            <Button variant="outline-danger" size="sm" className="ms-auto" onClick={() => handleDelete(build.id, build.buildName)}>
                                <FaTrash />
                            </Button>
                        </div>
                    </Card.Body>
                </Card>
            </Col>
        ));
    };

    return (
        <Container className="py-5">
            <Row className="mb-4 align-items-center">
                <Col>
                    <h1 className="mb-0">My Computer Builds</h1>
                </Col>
                <Col xs="auto">
                    <Button as={Link} to="/build/new" variant="success" size="lg">
                        <FaPlus className="me-2" /> สร้าง Build ใหม่
                    </Button>
                </Col>
            </Row>
            <Row>
                {renderBuilds()}
            </Row>
        </Container>
    );
};

export default MyBuilds;