import React, { useState } from 'react';
import { Modal, Button, Form } from 'react-bootstrap';

function ReasonModal({ show, onHide, onSubmit, title, label, placeholder }) {
    const [reason, setReason] = useState('');
    const [validated, setValidated] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();
        const form = e.currentTarget;
        if (form.checkValidity() === false) {
            e.stopPropagation();
        } else {
            onSubmit(reason);
            handleClose();
        }
        setValidated(true);
    };

    const handleClose = () => {
        setReason('');
        setValidated(false);
        onHide();
    }

    return (
        <Modal show={show} onHide={handleClose} centered>
            <Form noValidate validated={validated} onSubmit={handleSubmit}>
                <Modal.Header closeButton>
                    <Modal.Title>{title}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form.Group>
                        <Form.Label>{label}</Form.Label>
                        <Form.Control
                            as="textarea"
                            rows={3}
                            placeholder={placeholder}
                            required
                            value={reason}
                            onChange={(e) => setReason(e.target.value)}
                        />
                        <Form.Control.Feedback type="invalid">
                            A reason is required.
                        </Form.Control.Feedback>
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose}>
                        Cancel
                    </Button>
                    <Button variant="primary" type="submit">
                        Submit
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
}

export default ReasonModal;