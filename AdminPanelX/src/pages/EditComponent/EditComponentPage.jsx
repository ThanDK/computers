import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { Form, Button, Row, Col, Spinner, Card } from 'react-bootstrap';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { getComponentById, updateComponent } from '../../services/ComponentService';
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
    renderField,
    renderBrandSelect
} from '../../config/ComponentFormConfig.jsx';

import './EditComponentPage.css';

function EditComponentPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const { token } = useAuth();
    const queryClient = useQueryClient();

    const fromLocation = location.state?.from || { pathname: '/components' };

    // ดึงข้อมูล lookups (cache 5 นาที)
    const { data: lookups, isLoading: isLoadingLookups } = useQuery({
        queryKey: ['lookups'],
        queryFn: () => fetchAllLookups(token),
        enabled: !!token,
        staleTime: 300000,
    });

    // ดึงข้อมูล component, รอ lookups โหลดเสร็จก่อน
    const { data: componentData, isLoading: isLoadingComponent } = useQuery({
        queryKey: ['component', id],
        queryFn: async () => {
            const data = await getComponentById(id, token);
            // ถ้ามี brandName แต่ไม่มี brandId, ให้หา id จาก lookups
            if (data.brandName && !data.brandId && lookups) {
                const foundBrand = lookups.brands.find(b => b.name === data.brandName);
                if (foundBrand) {
                    data.brandId = foundBrand.id;
                }
            }
            return data;
        },
        enabled: !!token && !!id && !!lookups,
        onError: (err) => {
             notifyError("Failed to load component data. It may have been deleted.");
             console.error(err);
        }
    });

    // จัดการอัปเดต component
    const updateComponentMutation = useMutation({
        mutationFn: (variables) => updateComponent(
            variables.id,
            variables.updateData,
            variables.imageFile,
            variables.removeImage,
            variables.token
        ),
        onSuccess: () => {
            notifySuccess('Component updated successfully!');
            // refresh query ของ component นี้ และของ list ทั้งหมด
            queryClient.invalidateQueries({ queryKey: ['component', id] });
            queryClient.invalidateQueries({ queryKey: ['components'] });
            navigate(fromLocation);
        },
        onError: (err) => {
            notifyError(err.message || 'An unexpected error occurred.');
        }
    });

    const [componentType, setComponentType] = useState('');
    const [formData, setFormData] = useState({});
    const [imageFile, setImageFile] = useState(null);
    const [imagePreviewUrl, setImagePreviewUrl] = useState('');
    const [originalImageSrc, setOriginalImageSrc] = useState('');
    const [removeImage, setRemoveImage] = useState(false);
    const [cropModalState, setCropModalState] = useState({ show: false, src: '' });
    const fileInputRef = useRef(null);

    // อัปเดต state ของฟอร์มเมื่อ data โหลดเสร็จ
    useEffect(() => {
        if (componentData) {
            setFormData(componentData);
            setComponentType(componentData.type);
            if (componentData.imageUrl) {
                setImagePreviewUrl(componentData.imageUrl);
            }
        }
    }, [componentData]);
    
    // cleanup blob URL ป้องกัน memory leak
    useEffect(() => {
        return () => {
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
        };
    }, [imagePreviewUrl]);
    
    const handleChange = useCallback((e) => {
        const { name, value } = e.target;
        if (name === 'brandName') {
            const selectedBrand = lookups?.brands.find(b => b.name === value);
            setFormData(prev => ({ ...prev, brandName: value, brandId: selectedBrand ? selectedBrand.id : '' }));
        } else {
            setFormData(prev => ({ ...prev, [name]: value }));
        }
    }, [lookups]);

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
        if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
            URL.revokeObjectURL(imagePreviewUrl);
        }

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
        if (fileInputRef.current) {
            fileInputRef.current.value = "";
        }
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
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
            setImagePreviewUrl(URL.createObjectURL(croppedFile));
        }
        setCropModalState({ show: false, src: '' });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        const validationErrors = validateComponentData(formData, componentType);
        if (validationErrors.length > 0) {
            notifyError(validationErrors.join('\n'));
            return;
        }
        
        const { quantity, isActive, brandName, ...updateData } = formData;
        
        updateComponentMutation.mutate({
            id,
            updateData,
            imageFile,
            removeImage,
            token
        });
    };

    const isLoading = isLoadingLookups || isLoadingComponent;
    
    if (isLoading) {
        return (
            <div className="text-center p-5">
                <Spinner animation="border" variant="light" />
            </div>
        );
    }
    
    const typeLabel = componentTypes.find(t => t.value === componentType)?.label || "Component";

    return (
        <>
            <MainHeader />
            <PageHeader
                title={`Edit ${typeLabel}`}
                subtitle={`Editing component with MPN: ${formData.mpn || 'N/A'}`}
                showBackButton={true}
                onBack={() => navigate(fromLocation)}
            />

            <Card className="form-card">
                <Card.Body>
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
                            {renderField("mpn", "MPN", { value: formData.mpn, onChange: handleChange })}
                        </Row>
                        <Row>
                            {lookups && renderBrandSelect({ formData, lookups, onChange: handleChange })}
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
                                            <Button variant="secondary" size="sm" onClick={handleOpenCropper} disabled={!imageFile}>
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
                        <h5 className="section-header">Specific Details for {typeLabel}</h5>
                        
                        {lookups && COMPONENT_CONFIG[componentType]?.render({
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
                            disabled={updateComponentMutation.isPending}
                            className="mt-4 w-100"
                        >
                            {updateComponentMutation.isPending ? (
                                <><Spinner as="span" animation="border" size="sm" /> Saving...</>
                            ) : (
                                'Save Changes'
                            )}
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