// src/pages/EditComponentPage/EditComponentPage.jsx

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Form, Button, Row, Col, Spinner, Card, Alert } from 'react-bootstrap';

// Import Services and Components
import { getComponentById, updateComponent } from '../../services/ComponentService';
import { fetchAllLookups } from '../../services/LookupService';
import { notifySuccess } from '../../services/NotificationService';
import { useAuth } from '../../context/AuthContext';
import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ImageCropper from '../../components/ImageCropper/ImageCropper';
import './EditComponentPage.css';

// --- FIX: Add the .jsx extension to the import path. ---
import {
    COMPONENT_CONFIG,
    componentTypes,
    renderField
} from '../../config/ComponentFormConfig.jsx';

function EditComponentPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { token } = useAuth();

    // --- State Management ---
    const [componentType, setComponentType] = useState('');
    const [formData, setFormData] = useState({});
    const [lookups, setLookups] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [imageFile, setImageFile] = useState(null);
    const [imagePreviewUrl, setImagePreviewUrl] = useState('');
    const [originalImageSrc, setOriginalImageSrc] = useState('');
    const [removeImage, setRemoveImage] = useState(false);
    const [cropModalState, setCropModalState] = useState({ show: false, src: '' });

    // --- Hooks and Refs ---
    const fileInputRef = useRef(null);

    // --- Side Effects (useEffect) ---
    useEffect(() => {
        const fetchData = async () => {
            if (!token || !id) return;
            setIsLoading(true);
            setError('');
            try {
                const [componentData, lookupData] = await Promise.all([
                    getComponentById(id, token),
                    fetchAllLookups(token)
                ]);
                setLookups(lookupData);
                setFormData(componentData);
                setComponentType(componentData.type);
                if (componentData.imageUrl) {
                    setImagePreviewUrl(componentData.imageUrl);
                }
            } catch (err) {
                setError("Failed to load component data. It may have been deleted or an error occurred.");
                console.error(err);
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();
    }, [id, token]);
    
    useEffect(() => {
        return () => {
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
        };
    }, [imagePreviewUrl]);
    
    // --- Event Handlers ---
    const handleChange = useCallback((e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    }, []);

    const handleTagAdd = useCallback((fieldName, value) => {
        setFormData(prev => ({ ...prev, [fieldName]: [...(prev[fieldName] || []), value] }));
    }, []);

    const handleTagRemove = useCallback((fieldName, value) => {
        setFormData(prev => ({ ...prev, [fieldName]: prev[fieldName].filter(item => item !== value) }));
    }, []);

    const handleFileChange = (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setImageFile(file);
        setRemoveImage(false);
        if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) URL.revokeObjectURL(imagePreviewUrl);
        setImagePreviewUrl(URL.createObjectURL(file));
        const reader = new FileReader();
        reader.onloadend = () => setOriginalImageSrc(reader.result?.toString() || '');
        reader.readAsDataURL(file);
    };
    
    const handleRemoveImage = () => {
        setImageFile(null);
        setImagePreviewUrl('');
        setOriginalImageSrc('');
        setRemoveImage(true);
        if (fileInputRef.current) fileInputRef.current.value = "";
    };

    const handleOpenCropper = () => {
        if (originalImageSrc) {
            setCropModalState({ show: true, src: originalImageSrc });
        } else {
            alert("Cropping is only available for newly uploaded images.");
        }
    };
    
    const handleCropComplete = (croppedFile) => {
        if (croppedFile) {
            setImageFile(croppedFile);
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) URL.revokeObjectURL(imagePreviewUrl);
            setImagePreviewUrl(URL.createObjectURL(croppedFile));
        }
        setCropModalState({ show: false, src: '' });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsSubmitting(true);
        try {
            const { quantity, isActive, ...updateData } = formData; 
            await updateComponent(id, updateData, imageFile, removeImage, token);
            notifySuccess('Component updated successfully!');
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
    
    const typeLabel = componentTypes.find(t => t.value === componentType)?.label || "Component";

    return (
        <>
            <MainHeader />
            <PageHeader title={`Edit ${typeLabel}`} subtitle={`Editing component with MPN: ${formData.mpn || 'N/A'}`} />

            <Card className="form-card">
                <Card.Body>
                    {error && <Alert variant="danger">{error}</Alert>}
                    <Form noValidate onSubmit={handleSubmit}>
                        <Row className="mb-4">
                            <Form.Group as={Col} md="6" lg="4">
                                <Form.Label className="step-label">Component Type</Form.Label>
                                <Form.Control type="text" value={typeLabel} readOnly disabled />
                            </Form.Group>
                        </Row>
                        
                        <h5 className="section-header">Common Details</h5>
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
                            {renderField("price", "Price (฿)", { type: "number", md: 12, value: formData.price, onChange: handleChange })}
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
                                            <Button variant="secondary" size="sm" onClick={handleOpenCropper} disabled={!imageFile}>Crop</Button>
                                            <Button variant="outline-danger" size="sm" onClick={handleRemoveImage}>Remove</Button>
                                        </div>
                                    </div>
                                )}
                            </Form.Group>
                        </Row>

                        <hr className="form-divider my-4" />

                        <h5 className="section-header">Specific Details for {typeLabel}</h5>
                        {COMPONENT_CONFIG[componentType]?.render({
                            formData,
                            lookups,
                            handleChange,
                            handleTagAdd,
                            handleTagRemove
                        })}

                        <Button type="submit" variant="primary" size="lg" disabled={isSubmitting} className="mt-4 w-100">
                            {isSubmitting ? <><Spinner as="span" animation="border" size="sm" /> Saving...</> : 'Save Changes'}
                        </Button>
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

export default EditComponentPage;