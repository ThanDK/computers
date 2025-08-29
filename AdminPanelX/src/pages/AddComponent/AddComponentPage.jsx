import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Button, Row, Col, Spinner, Card } from 'react-bootstrap';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { createComponent } from '../../services/ComponentService';
import { fetchAllLookups } from '../../services/LookupService';
import { notifySuccess, notifyError } from '../../services/NotificationService';
import { validateComponentData } from '../../services/ValidationService';
import { useAuth } from '../../context/AuthContext';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ImageCropper from '../../components/ImageCropper/ImageCropper';

import {
    COMPONENT_CONFIG,
    componentTypes,
    renderField
} from '../../config/ComponentFormConfig.jsx';

import './AddComponentPage.css';

function AddComponentPage() {
    const [selectedType, setSelectedType] = useState('');
    const [formData, setFormData] = useState({});
    const [imageFile, setImageFile] = useState(null);
    const [imagePreviewUrl, setImagePreviewUrl] = useState('');
    const [originalImageSrc, setOriginalImageSrc] = useState('');
    const [cropModalState, setCropModalState] = useState({ show: false, src: '' });

    const fileInputRef = useRef(null);
    const { token } = useAuth();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    // ดึงข้อมูล lookups (พวก brand, socket, etc.) สำหรับใช้ในฟอร์ม
    // staleTime 5 นาที เพื่อลดการ fetch ซ้ำซ้อนเวลาสลับหน้าไปมา
    const { data: lookups, isLoading } = useQuery({
        queryKey: ['lookups'],
        queryFn: () => fetchAllLookups(token),
        enabled: !!token,
        staleTime: 300000,
        onError: (err) => {
            notifyError("Could not load form data. Please try again later.");
            console.error(err);
        }
    });

    // จัดการ logic การสร้าง component ใหม่
    const createComponentMutation = useMutation({
        mutationFn: (variables) => createComponent(
            variables.componentData,
            variables.imageFile,
            variables.token
        ),
        onSuccess: () => {
            notifySuccess('Component created successfully!');
            // เมื่อสร้างสำเร็จ, สั่งให้ query 'components' (หน้า list) refresh ใหม่
            queryClient.invalidateQueries({ queryKey: ['components'] });
            navigate('/components');
        },
        onError: (err) => {
            notifyError(err.message || 'An unexpected error occurred.');
        }
    });

    // Cleanup blob URL ที่สร้างจาก `URL.createObjectURL` เพื่อป้องกัน memory leak
    useEffect(() => {
        return () => {
            if (imagePreviewUrl) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
        };
    }, [imagePreviewUrl]);

    // รีเซ็ตฟอร์มทุกครั้งที่ user เปลี่ยน Component Type
    const handleTypeChange = (e) => {
        const type = e.target.value;
        setSelectedType(type);
        const baseState = { name: "", mpn: "", description: "", price: "", quantity: "", brandId: "" };
        const specificState = COMPONENT_CONFIG[type]?.initialState || {};
        setFormData({ ...baseState, ...specificState });
    };
    
    // ใช้ useCallback เพื่อ performance, ป้องกันการสร้างฟังก์ชันใหม่ทุกครั้งที่ re-render
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
        if (imagePreviewUrl) {
            URL.revokeObjectURL(imagePreviewUrl);
        }

        const newPreviewUrl = URL.createObjectURL(file);
        setImagePreviewUrl(newPreviewUrl);

        // อ่านไฟล์ภาพเพื่อใช้ใน cropper
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
            if (imagePreviewUrl) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
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

        const validationErrors = validateComponentData(formData, selectedType);
        if (validationErrors.length > 0) {
            notifyError(validationErrors.join('\n'));
            return;
        }
        
        const componentData = { type: selectedType, ...formData };
        createComponentMutation.mutate({ componentData, imageFile, token });
    };
    
    if (isLoading) {
        return (
            <div className="text-center p-5">
                <Spinner animation="border" variant="light" />
            </div>
        );
    }
    
    return (
        <>
            <MainHeader />
            <PageHeader
                title="Add New Component"
                subtitle="Fill out the form to add a new product component"
                showBackButton={true}
                onBack={() => navigate('/components')}
            />

            <Card className="form-card">
                <Card.Body>
                    <Form noValidate onSubmit={handleSubmit}>
                        <Row className="mb-4">
                            <Form.Group as={Col} md="6" lg="4">
                                <Form.Label className="step-label">1. Select Component Type</Form.Label>
                                <Form.Select
                                    value={selectedType}
                                    onChange={handleTypeChange}
                                    disabled={createComponentMutation.isPending}
                                    required
                                >
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
                                    {renderField("mpn", "MPN", { value: formData.mpn, onChange: handleChange })}
                                </Row>

                                <Row className="mt-3">
                                     <Form.Group as={Col} md={6}>
                                        <Form.Label>Brand</Form.Label>
                                        <Form.Select
                                            name="brandId"
                                            value={formData.brandId || ''}
                                            onChange={handleChange}
                                            disabled={!lookups?.brands}
                                            required
                                        >
                                            <option value="">-- Select a Brand --</option>
                                            {lookups?.brands?.map(brand => (
                                                <option key={brand.id} value={brand.id}>
                                                    {brand.name}
                                                </option>
                                            ))}
                                        </Form.Select>
                                    </Form.Group>
                                </Row>

                                <Row className="mt-3">
                                    <Form.Group as={Col}>
                                        <Form.Label>Description</Form.Label>
                                        <Form.Control
                                            as="textarea"
                                            rows={3}
                                            name="description"
                                            value={formData.description || ''}
                                            onChange={handleChange}
                                        />
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
                                            <Form.Control
                                                type="file"
                                                ref={fileInputRef}
                                                accept="image/*"
                                                onChange={handleFileChange}
                                            />
                                        ) : (
                                            <div className="image-preview-container-center">
                                                <img src={imagePreviewUrl} alt="Component Preview" className="image-preview"/>
                                                <div className="image-actions">
                                                    <Button variant="secondary" size="sm" onClick={handleOpenCropper}>
                                                        Crop
                                                    </Button>
                                                    <Button variant="outline-danger" size="sm" onClick={handleRemoveImage}>
                                                        Remove
                                                    </Button>
                                                </div>
                                            </div>
                                        )}
                                    </Form.Group>
                                </Row>
                                
                                <hr className="form-divider my-4" />

                                <h5 className="section-header">
                                    3. Specific Details for {selectedType.charAt(0).toUpperCase() + selectedType.slice(1)}
                                </h5>
                                
                                {lookups && COMPONENT_CONFIG[selectedType]?.render({
                                    formData,
                                    lookups,
                                    handleChange,
                                    handleTagAdd,
                                    handleTagRemove
                                })}

                                <Button
                                    type="submit"
                                    variant="primary"
                                    size="lg"
                                    disabled={createComponentMutation.isPending}
                                    className="mt-4 w-100"
                                >
                                    {createComponentMutation.isPending ? (
                                        <><Spinner as="span" animation="border" size="sm" /> Creating...</>
                                    ) : (
                                        'Create Component'
                                    )}
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