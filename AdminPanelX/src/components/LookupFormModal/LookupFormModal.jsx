import React, { useState, useEffect, useRef } from 'react';
import { Modal, Form, Button, Row, Col, Image, Spinner } from 'react-bootstrap';
import ImageCropper from '../ImageCropper/ImageCropper';

function LookupFormModal({ show, onHide, onSubmit, isSubmitting, modalState, activeTab, lookupConfig, formFactorTypes }) {
  const [imageFile, setImageFile] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
  const [originalImageSrc, setOriginalImageSrc] = useState('');
  const [cropModalState, setCropModalState] = useState({ show: false, src: '' });
  const fileInputRef = useRef(null);
  const { currentItem } = modalState;

  useEffect(() => {
    if (show) {
      const existingUrl = currentItem?.logoUrl || null;
      setImagePreviewUrl(existingUrl);
      setOriginalImageSrc(existingUrl);
    }
  }, [show, currentItem]);

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

  const renderModalFormBody = () => {
    const config = lookupConfig[activeTab];
    if (!config) return null;
    if (config.hasImage) {
      return (
        <Row>
          <Col>
            <div className="image-preview-container d-flex flex-column align-items-center mb-3">
              {imagePreviewUrl ? (
                <>
                  <Image src={imagePreviewUrl} alt="Logo preview" className="logo-preview" />
                  <div className="image-actions d-flex gap-2 mt-2">
                    <Button variant="secondary" size="sm" onClick={handleOpenCropper}>Crop</Button>
                    <Button variant="outline-danger" size="sm" onClick={handleRemoveImage}>Remove</Button>
                  </div>
                </>
              ) : (
                <span className="image-preview-placeholder">No image selected</span>
              )}
            </div>
            <Form.Group className="mb-3">
              <Form.Label>Brand Name</Form.Label>
              <Form.Control name="name" type="text" required defaultValue={currentItem?.name || ''} autoFocus />
            </Form.Group>
            <Form.Group>
              <Form.Label>Brand Logo</Form.Label>
              <Form.Control name="image" type="file" accept="image/*" onChange={handleImageChange} ref={fileInputRef} />
            </Form.Group>
          </Col>
        </Row>
      );
    }
    return (
      <Row>
        {config.fields.map((field, index) => (
          <Col md={12} key={field}>
            <Form.Group className="mb-3">
              <Form.Label>{field.charAt(0).toUpperCase() + field.slice(1)}</Form.Label>
              {field === 'type' ? (
                <Form.Select name="type" required defaultValue={currentItem?.type || ''}>
                  <option value="" disabled>-- Select Type --</option>
                  {formFactorTypes.map((type) => (<option key={type} value={type}>{type}</option>))}
                </Form.Select>
              ) : (
                <Form.Control type="text" name={field} required autoFocus={index === 0} defaultValue={currentItem?.[field] || ''} />
              )}
            </Form.Group>
          </Col>
        ))}
      </Row>
    );
  };

  return (
    <>
      <Modal show={show} onHide={onHide} centered className="form-modal">
        <Modal.Header closeButton>
          <Modal.Title>{modalState.type === 'add' ? 'Add New' : 'Edit'} {lookupConfig[activeTab]?.title.slice(0, -1)}</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleInternalSubmit}>
          <Modal.Body>{renderModalFormBody()}</Modal.Body>
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

export default LookupFormModal;