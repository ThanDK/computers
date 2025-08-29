import React, { useState, useEffect, useRef } from 'react';
import { Modal, Form, Button, Row, Col, Image, Spinner } from 'react-bootstrap';
import ImageCropper from '../ImageCropper/ImageCropper';

function ShippingProviderFormModal({ show, onHide, provider, onSubmit, isSubmitting }) {
  const [imageFile, setImageFile] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
  const [originalImageSrc, setOriginalImageSrc] = useState('');
  const [cropModalState, setCropModalState] = useState({ show: false, src: '' });
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (show) {
      const existingUrl = provider?.imageUrl || null;
      setImagePreviewUrl(existingUrl);
      setOriginalImageSrc(existingUrl);
      setImageFile(null); 
    }
  }, [show, provider]);

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
    onSubmit(formData, imageFile);
  };

  return (
    <>
      <Modal show={show} onHide={onHide} centered className="form-modal">
        <Modal.Header closeButton>
          <Modal.Title>{provider ? 'Edit' : 'Add New'} Shipping Provider</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleInternalSubmit}>
          <Modal.Body>
            <Row>
              <Col>
                <div className="image-preview-container d-flex flex-column align-items-center mb-3">
                  {imagePreviewUrl ? (
                    <>
                      <Image src={imagePreviewUrl} alt="Provider logo preview" className="logo-preview" />
                      <div className="image-actions d-flex gap-2 mt-2">
                        <Button variant="secondary" size="sm" onClick={handleOpenCropper} disabled={!imageFile}>Crop</Button>
                        <Button variant="outline-danger" size="sm" onClick={handleRemoveImage}>Remove</Button>
                      </div>
                    </>
                  ) : (
                    <span className="image-preview-placeholder">No image selected</span>
                  )}
                </div>
                <Form.Group className="mb-3">
                  <Form.Label>Provider Name</Form.Label>
                  <Form.Control name="name" type="text" required defaultValue={provider?.name || ''} autoFocus />
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>Tracking URL (use "{'{trackingNumber}'}" as placeholder)</Form.Label>
                  <Form.Control name="trackingUrl" type="text" placeholder="e.g., https://..." defaultValue={provider?.trackingUrl || ''} />
                </Form.Group>
                <Form.Group>
                  <Form.Label>Provider Logo</Form.Label>
                  <Form.Control name="image" type="file" accept="image/*" onChange={handleImageChange} ref={fileInputRef} />
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

      <ImageCropper show={cropModalState.show} imageSrc={cropModalState.src} onHide={() => setCropModalState({ show: false, src: '' })} onCropComplete={handleCropComplete} aspect={1} />
    </>
  );
}

export default ShippingProviderFormModal;