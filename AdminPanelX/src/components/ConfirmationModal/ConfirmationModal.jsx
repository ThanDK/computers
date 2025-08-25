import React from 'react';
import { Modal, Button } from 'react-bootstrap';

// Component นี้สร้างมาเพื่อให้เป็น Modal ยืนยันที่ใช้ซ้ำได้ง่ายๆ
function ConfirmationModal({ show, onHide, onConfirm, title, children, confirmText = 'Confirm', confirmVariant = 'primary' }) {
    return (
        <Modal show={show} onHide={onHide} centered>
            <Modal.Header closeButton>
                <Modal.Title>{title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {/* children คือ content ที่จะเอามาแสดงใน modal body สามารถส่งอะไรเข้ามาก็ได้ */}
                {children}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={onHide}>
                    Cancel
                </Button>
                {/* ทำให้ปุ่มยืนยันเปลี่ยนสีและข้อความได้จาก props ที่ส่งเข้ามา */}
                <Button variant={confirmVariant} onClick={onConfirm}>
                    {confirmText}
                </Button>
            </Modal.Footer>
        </Modal>
    );
}

export default ConfirmationModal;