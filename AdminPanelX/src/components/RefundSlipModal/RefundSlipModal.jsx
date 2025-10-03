import React, { useState, useEffect, useRef } from 'react';
import { Modal, Form, Button, Image, Spinner, Alert } from 'react-bootstrap';
import ImageCropper from '../ImageCropper/ImageCropper';

function RefundSlipModal({ show, onHide, onSubmit, isSubmitting, title, confirmText }) {
    const [slipFile, setSlipFile] = useState(null);
    const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
    const [originalImageSrc, setOriginalImageSrc] = useState('');
    const [cropModalState, setCropModalState] = useState({ show: false, src: '' });
    const fileInputRef = useRef(null);

    useEffect(() => {
        if (show) {
            setSlipFile(null);
            setImagePreviewUrl(null);
            setOriginalImageSrc('');
        }
    }, [show]);

    useEffect(() => {
        return () => {
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
        };
    }, [imagePreviewUrl]);

    const handleImageChange = (event) => {
        const file = event.target.files[0];
        if (file) {
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
            setSlipFile(file);
            setImagePreviewUrl(URL.createObjectURL(file));
            const reader = new FileReader();
            reader.onloadend = () => setOriginalImageSrc(reader.result?.toString() || '');
            reader.readAsDataURL(file);
        }
    };

    const handleOpenCropper = () => {
        if (originalImageSrc) {
            setCropModalState({ show: true, src: originalImageSrc });
        }
    };

    const handleCropComplete = (croppedFile) => {
        if (croppedFile) {
            setSlipFile(croppedFile);
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
            setImagePreviewUrl(URL.createObjectURL(croppedFile));
        }
        setCropModalState({ show: false, src: '' });
    };

    const handleRemoveImage = () => {
        setSlipFile(null);
        if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
            URL.revokeObjectURL(imagePreviewUrl);
        }
        setImagePreviewUrl(null);
        setOriginalImageSrc('');
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleInternalSubmit = (event) => {
        event.preventDefault();
        if (slipFile) {
            onSubmit(slipFile);
        }
    };

    return (
        <>
            <Modal show={show} onHide={onHide} centered className="form-modal">
                <Modal.Header closeButton>
                    <Modal.Title>{title}</Modal.Title>
                </Modal.Header>
                <Form onSubmit={handleInternalSubmit}>
                    <Modal.Body>
                        <Alert variant="info">
                            To complete the refund, please upload the proof of payment.
                        </Alert>
                        <div className="image-preview-container d-flex flex-column align-items-center mb-3">
                            {imagePreviewUrl ? (
                                <>
                                    <Image src={imagePreviewUrl} alt="Refund slip preview" className="logo-preview" />
                                    <div className="image-actions d-flex gap-2 mt-2">
                                        <Button variant="secondary" size="sm" onClick={handleOpenCropper}>Crop</Button>
                                        <Button variant="outline-danger" size="sm" onClick={handleRemoveImage}>Remove</Button>
                                    </div>
                                </>
                            ) : (
                                <span className="image-preview-placeholder">No image selected</span>
                            )}
                        </div>
                        <Form.Group>
                            <Form.Label>Refund Slip</Form.Label>
                            <Form.Control
                                name="refundSlip"
                                type="file"
                                accept="image/*"
                                onChange={handleImageChange}
                                ref={fileInputRef}
                                required
                            />
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={onHide}>Cancel</Button>
                        <Button variant="primary" type="submit" disabled={isSubmitting || !slipFile}>
                            {isSubmitting ? (<><Spinner as="span" animation="border" size="sm" /> Processing...</>) : confirmText}
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>

            <ImageCropper
                show={cropModalState.show}
                imageSrc={cropModalState.src}
                onHide={() => setCropModalState({ show: false, src: '' })}
                onCropComplete={handleCropComplete}
            />
        </>
    );
}

export default RefundSlipModal;