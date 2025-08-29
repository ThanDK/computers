import React, { useState, useEffect, useRef } from 'react';
import { Modal, Form, Button, Row, Col, Image, Spinner } from 'react-bootstrap';
import ImageCropper from '../ImageCropper/ImageCropper';

// Modal นี้ถูกออกแบบมาให้ใช้ซ้ำได้ทั้งกับ lookup ที่เป็น text ธรรมดา และ 'Brands' ที่มีรูปภาพ
function LookupFormModal({ show, onHide, onSubmit, isSubmitting, modalState, activeTab, lookupConfig, formFactorTypes }) {
  // state ชุดนี้ใช้สำหรับจัดการเรื่องรูปภาพโดยเฉพาะ
  const [imageFile, setImageFile] = useState(null); // เก็บ File object สุดท้ายที่จะอัปโหลด
  const [imagePreviewUrl, setImagePreviewUrl] = useState(null); // เก็บ URL สำหรับโชว์ใน <img>
  const [originalImageSrc, setOriginalImageSrc] = useState(''); // เก็บ base64 string ของรูปต้นฉบับ ไว้ส่งให้ cropper
  const [cropModalState, setCropModalState] = useState({ show: false, src: '' }); // state เปิด/ปิด modal ครอปรูป
  const fileInputRef = useRef(null);
  const { currentItem } = modalState;

  // ใช้ effect นี้เพื่อเซ็ตค่าเริ่มต้นของรูปภาพ ตอนที่ modal เปิดขึ้นมาในโหมด edit
  useEffect(() => {
    if (show) {
      const existingUrl = currentItem?.logoUrl || null;
      setImagePreviewUrl(existingUrl);
      setOriginalImageSrc(existingUrl);
    }
  }, [show, currentItem]);

  // ใช้ effect นี้ cleanup กัน memory leak ตอนที่ imagePreviewUrl เป็น URL แบบ blob ที่สร้างขึ้นมาเอง
  useEffect(() => {
    return () => {
      if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
    };
  }, [imagePreviewUrl]);

  // ตอนเลือกไฟล์ใหม่ ก็จะสร้าง URL สำหรับ preview และ base64 สำหรับ cropper
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

  // ฟังก์ชัน callback ที่รับไฟล์ซึ่งครอปเสร็จแล้วจาก ImageCropper มาอัปเดต state
  const handleCropComplete = (croppedFile) => {
    if (croppedFile) {
      setImageFile(croppedFile);
      if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) URL.revokeObjectURL(imagePreviewUrl);
      setImagePreviewUrl(URL.createObjectURL(croppedFile));
    }
    setCropModalState({ show: false, src: '' });
  };

  // จัดการตอนกดปุ่มลบรูปภาพ ก็จะเคลียร์ state ทั้งหมดที่เกี่ยวกับรูป
  const handleRemoveImage = () => {
    setImageFile(null);
    if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) URL.revokeObjectURL(imagePreviewUrl);
    setImagePreviewUrl(null);
    setOriginalImageSrc('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // ฟังก์ชันนี้จะรวบรวมข้อมูลจากฟอร์ม แล้วส่ง formData กับ imageFile กลับไปให้ parent component
  const handleInternalSubmit = (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    onSubmit(formData, imageFile); 
  };

  // ฟังก์ชันนี้จะ render body ของฟอร์มตาม lookupConfig ที่ถูกส่งเข้ามา
  const renderModalFormBody = () => {
    const config = lookupConfig[activeTab];
    if (!config) return null;

    // ส่วนนี้สำหรับ render ฟอร์มของ 'Brands' ที่มีเรื่องรูปภาพเข้ามาเกี่ยว
    if (config.hasImage) {
      return (
        <Row>
          <Col>
            <div className="image-preview-container d-flex flex-column align-items-center mb-3">
              {imagePreviewUrl ? (
                <>
                  <Image src={imagePreviewUrl} alt="Logo preview" className="logo-preview" />
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
    // ส่วนนี้สำหรับ render ฟอร์ม lookup ทั่วไป ที่มีแค่ text field
    return (
      <Row>
        {config.fields.map((field, index) => (
          <Col md={12} key={field}>
            <Form.Group className="mb-3">
              <Form.Label>{field.charAt(0).toUpperCase() + field.slice(1)}</Form.Label>
              {/* เช็คเงื่อนไขพิเศษ: ถ้า field ชื่อ 'type' ให้ render เป็น Form.Select แทน */}
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

      {/* ตัว Modal ที่ใช้ครอปรูปภาพ จะถูก render ไว้ตรงนี้ แต่จะแสดงผลก็ต่อเมื่อ state ถูกสั่งให้โชว์ */}
      <ImageCropper show={cropModalState.show} imageSrc={cropModalState.src} onHide={() => setCropModalState({ show: false, src: '' })} onCropComplete={handleCropComplete} aspect={1} />
    </>
  );
}

export default LookupFormModal;