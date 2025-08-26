import React from 'react';
import { Modal } from 'react-bootstrap';
import './ImageModal.css';

function ImageModal({ show, onHide, imageUrl, altText = 'Fullscreen image' }) {
  // ถ้าไม่มี URL ของรูปภาพส่งเข้ามา ก็ไม่ต้องแสดง component นี้
  if (!imageUrl) {
    return null;
  }

  return (
    <Modal show={show} onHide={onHide} centered size="lg" className="image-modal">
      <Modal.Header closeButton></Modal.Header>
      <Modal.Body>
        <img src={imageUrl} alt={altText} className="fullscreen-image" />
      </Modal.Body>
    </Modal>
  );
}

export default ImageModal;