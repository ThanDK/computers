import React from 'react';
import { Modal, Button } from 'react-bootstrap';

function ConfirmationModal({ show, onHide, onConfirm, title, body, children, confirmText = 'Confirm', confirmVariant = 'primary' }) {
    return (
        <Modal show={show} onHide={onHide} centered>
            <Modal.Header closeButton>
                <Modal.Title>{title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {/* FIX: Ensure the 'body' prop is rendered here. */}
                {body || children}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={onHide}>
                    Cancel
                </Button>
                <Button variant={confirmVariant} onClick={onConfirm}>
                    {confirmText}
                </Button>
            </Modal.Footer>
        </Modal>
    );
}

export default ConfirmationModal;