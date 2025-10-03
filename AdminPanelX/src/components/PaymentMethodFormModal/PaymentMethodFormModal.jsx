import React, { useState, useEffect, useRef } from 'react';
import { Modal, Form, Button, Row, Col, Image, Spinner } from 'react-bootstrap';
import ImageCropper from '../ImageCropper/ImageCropper';

function PaymentMethodFormModal({ show, onHide, method, onSubmit, isSubmitting }) {
  const [imageFile, setImageFile] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
  const [originalImageSrc, setOriginalImageSrc] = useState('');
  const [cropModalState, setCropModalState] = useState({ show: false, src: '' });
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (show) {
      const existingUrl = method?.qrCodeImageUrl || null;
      setImagePreviewUrl(existingUrl);
      setOriginalImageSrc(existingUrl);
      setImageFile(null); 
    }
  }, [show, method]);

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
      if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) URL.revokeObjectURL(imagePreviewUrl);
      setImageFile(file);
      setImagePreviewUrl(URL.createObjectURL(file));
      const reader = new FileReader();
      reader.onloadend = () => setOriginalImageSrc(reader.result?.toString() || '');
      reader.readAsDataURL(file);
    }
  };

  const handleOpenCropper = () => {
    if (originalImageSrc) setCropModalState({ show: true, src: originalImageSrc });
  };

  const handleCropComplete = (croppedFile) => {
    if (croppedFile) {
      setImageFile(croppedFile);
      if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) URL.revokeObjectURL(imagePreviewUrl);
      setImagePreviewUrl(URL.createObjectURL(croppedFile));
    }
    setCropModalState({ show: false, src: '' });
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) URL.revokeObjectURL(imagePreviewUrl);
    setImagePreviewUrl(null);
    setOriginalImageSrc('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };
  
  const handleInternalSubmit = (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const requestData = Object.fromEntries(formData.entries());
    delete requestData.image; // Remove image key from request data
    onSubmit(requestData, imageFile);
  };

  const isEditMode = !!method;

  return (
    <>
      <Modal show={show} onHide={onHide} centered className="form-modal">
        <Modal.Header closeButton>
          <Modal.Title>{isEditMode ? 'Edit' : 'Add New'} Payment Method</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleInternalSubmit}>
          <Modal.Body>
            <Row>
              <Col>
                <div className="image-preview-container d-flex flex-column align-items-center mb-3">
                  {imagePreviewUrl ? (
                    <>
                      <Image src={imagePreviewUrl} alt="QR Code preview" className="logo-preview" />
                      <div className="image-actions d-flex gap-2 mt-2">
                        <Button variant="secondary" size="sm" onClick={handleOpenCropper}>Crop</Button>
                        <Button variant="outline-danger" size="sm" onClick={handleRemoveImage}>Remove</Button>
                      </div>
                    </>
                  ) : (
                    <span className="image-preview-placeholder">No QR Code selected</span>
                  )}
                </div>
                <Form.Group className="mb-3">
                  <Form.Label>Bank Name</Form.Label>
                  <Form.Control name="bankName" type="text" required defaultValue={method?.bankName || ''} autoFocus />
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>Account Name</Form.Label>
                  <Form.Control name="accountName" type="text" required defaultValue={method?.accountName || ''} />
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>Account Number</Form.Label>
                  <Form.Control name="accountNumber" type="text" required defaultValue={method?.accountNumber || ''} />
                </Form.Group>
                <Form.Group>
                  <Form.Label>QR Code Image</Form.Label>
                  <Form.Control name="image" type="file" accept="image/*" onChange={handleImageChange} ref={fileInputRef} required={!isEditMode} />
                </Form.Group>
              </Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={onHide}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? (<><Spinner as="span" animation="border" size="sm" /> Saving...</>) : 'Save Changes'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ImageCropper show={cropModalState.show} imageSrc={cropModalState.src} onHide={() => setCropModalState({ show: false, src: '' })} onCropComplete={handleCropComplete} />
    </>
  );
}

export default PaymentMethodFormModal;