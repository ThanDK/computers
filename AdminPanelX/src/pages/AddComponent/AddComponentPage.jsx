// src/pages/AddComponentPage/AddComponentPage.jsx

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Button, Row, Col, Spinner, Card, Alert } from 'react-bootstrap';

// Import Services and Components
import { createComponent } from '../../services/ComponentService';
import { fetchAllLookups } from '../../services/LookupService';
import { notifySuccess } from '../../services/NotificationService';
import { useAuth } from '../../context/AuthContext';
import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ImageCropper from '../../components/ImageCropper/ImageCropper';
import './AddComponentPage.css';

// --- FIX: Add the .jsx extension to the import path. ---
import {
    COMPONENT_CONFIG,
    componentTypes,
    renderField
} from '../../config/ComponentFormConfig.jsx';


function AddComponentPage() {
    // --- State Management ---
    const [selectedType, setSelectedType] = useState('');
    const [formData, setFormData] = useState({});
    const [lookups, setLookups] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [imageFile, setImageFile] = useState(null);
    const [imagePreviewUrl, setImagePreviewUrl] = useState('');
    const [originalImageSrc, setOriginalImageSrc] = useState('');
    const [cropModalState, setCropModalState] = useState({ show: false, src: '' });

    // --- Hooks and Refs ---
    const fileInputRef = useRef(null);
    const { token } = useAuth();
    const navigate = useNavigate();

    // --- Side Effects (useEffect) ---
    useEffect(() => {
        const getLookups = async () => {
            if (!token) {
                setIsLoading(false);
                return;
            }
            try {
                const data = await fetchAllLookups(token);
                setLookups(data);
            } catch (err) {
                setError("Could not load form data. Please try again later.");
                console.error(err);
            } finally {
                setIsLoading(false);
            }
        };
        getLookups();
    }, [token]);

    useEffect(() => {
        return () => {
            if (imagePreviewUrl) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
        };
    }, [imagePreviewUrl]);

    // --- Event Handlers ---
    const handleTypeChange = (e) => {
        const type = e.target.value;
        setSelectedType(type);
        const baseState = { name: "", mpn: "", description: "", price: "", quantity: "" };
        const specificState = COMPONENT_CONFIG[type]?.initialState || {};
        setFormData({ ...baseState, ...specificState });
        setError('');
    };
    
    const handleChange = useCallback((e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    }, []);

    const handleTagAdd = useCallback((fieldName, value) => {
        setFormData(prev => ({
            ...prev,
            [fieldName]: [...(prev[fieldName] || []), value]
        }));
    }, []);

    const handleTagRemove = useCallback((fieldName, value) => {
        setFormData(prev => ({
            ...prev,
            [fieldName]: prev[fieldName].filter(item => item !== value)
        }));
    }, []);
    
    const handleFileChange = (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setImageFile(file);
        if (imagePreviewUrl) URL.revokeObjectURL(imagePreviewUrl);
        setImagePreviewUrl(URL.createObjectURL(file));
        const reader = new FileReader();
        reader.onloadend = () => setOriginalImageSrc(reader.result?.toString() || '');
        reader.readAsDataURL(file);
    };

    const handleOpenCropper = () => {
        if (originalImageSrc) {
            setCropModalState({ show: true, src: originalImageSrc });
        }
    };
    
    const handleCropComplete = (croppedFile) => {
        if (croppedFile) {
            setImageFile(croppedFile);
            if (imagePreviewUrl) URL.revokeObjectURL(imagePreviewUrl);
            setImagePreviewUrl(URL.createObjectURL(croppedFile));
        }
        setCropModalState({ show: false, src: '' });
    };

    const handleRemoveImage = () => {
        setImageFile(null);
        setImagePreviewUrl('');
        setOriginalImageSrc('');
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsSubmitting(true);
        try {
            await createComponent({ type: selectedType, ...formData }, imageFile, token);
            notifySuccess('Component created successfully!');
            navigate('/components');
        } catch (err) {
            setError(err.message || 'An unexpected error occurred. Please try again.');
        } finally {
            setIsSubmitting(false);
        }
    };
    
    // --- Render ---
    if (isLoading) {
        return <div className="text-center p-5"><Spinner animation="border" variant="light" /></div>;
    }
    
    return (
        <>
            <MainHeader />
            <PageHeader title="Add New Component" subtitle="Fill out the form to add a new product component" />

            <Card className="form-card">
                <Card.Body>
                    {error && <Alert variant="danger">{error}</Alert>}
                    <Form noValidate onSubmit={handleSubmit}>
                        <Row className="mb-4">
                            <Form.Group as={Col} md="6" lg="4">
                                <Form.Label className="step-label">1. Select Component Type</Form.Label>
                                <Form.Select value={selectedType} onChange={handleTypeChange} disabled={isSubmitting}>
                                    <option value="">-- Choose Type --</option>
                                    {componentTypes.map(type => (
                                        <option key={type.value} value={type.value}>{type.label}</option>
                                    ))}
                                </Form.Select>
                            </Form.Group>
                        </Row>

                        {selectedType && (
                            <>
                                <h5 className="section-header">2. Common Details</h5>
                                <Row>
                                    {renderField("name", "Component Name", { value: formData.name, onChange: handleChange })}
                                    {renderField("mpn", "MPN (Manufacturer Part Number)", { value: formData.mpn, onChange: handleChange })}
                                </Row>
                                <Row className="mt-3">
                                    <Form.Group as={Col}>
                                        <Form.Label>Description</Form.Label>
                                        <Form.Control as="textarea" rows={3} name="description" value={formData.description || ''} onChange={handleChange} />
                                    </Form.Group>
                                </Row>
                                <Row className="mt-3">
                                    {renderField("price", "Price (฿)", { type: "number", value: formData.price, onChange: handleChange })}
                                    {renderField("quantity", "Initial Stock", { type: "number", value: formData.quantity, onChange: handleChange })}
                                </Row>
                                <Row>
                                    <Form.Group as={Col} className="mt-3">
                                        <Form.Label>Component Image</Form.Label>
                                        {!imagePreviewUrl ? (
                                            <Form.Control type="file" ref={fileInputRef} accept="image/*" onChange={handleFileChange} />
                                        ) : (
                                            <div className="image-preview-container-center">
                                                <img src={imagePreviewUrl} alt="Component Preview" className="image-preview"/>
                                                <div className="image-actions">
                                                    <Button variant="secondary" size="sm" onClick={handleOpenCropper}>Crop</Button>
                                                    <Button variant="outline-danger" size="sm" onClick={handleRemoveImage}>Remove</Button>
                                                </div>
                                            </div>
                                        )}
                                    </Form.Group>
                                </Row>
                                
                                <hr className="form-divider my-4" />

                                <h5 className="section-header">3. Specific Details for {selectedType.charAt(0).toUpperCase() + selectedType.slice(1)}</h5>
                                {
                                    COMPONENT_CONFIG[selectedType]?.render({
                                        formData,
                                        lookups,
                                        handleChange,
                                        handleTagAdd,
                                        handleTagRemove
                                    })
                                }

                                <Button type="submit" variant="primary" size="lg" disabled={isSubmitting} className="mt-4 w-100">
                                    {isSubmitting ? <><Spinner as="span" animation="border" size="sm" /> Creating...</> : 'Create Component'}
                                </Button>
                            </>
                        )}
                    </Form>
                </Card.Body>
            </Card>

            <ImageCropper 
                show={cropModalState.show}
                imageSrc={cropModalState.src}
                onHide={() => setCropModalState({ show: false, src: '' })}
                onCropComplete={handleCropComplete}
                aspect={1}
            />
        </>
    );
}

export default AddComponentPage;