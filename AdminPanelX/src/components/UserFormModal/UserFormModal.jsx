import React from 'react';
import { Modal, Form, Button, Row, Col, Spinner } from 'react-bootstrap';

const roleOptions = ["ROLE_USER", "ROLE_ADMIN"];

function UserFormModal({ show, onHide, modalState, onSubmit, isSubmitting }) {
  
  const handleInternalSubmit = (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const data = Object.fromEntries(formData.entries());

    if (!data.password) {
      delete data.password;
    }
    onSubmit(data);
  };

  return (
    <Modal show={show} onHide={onHide} centered className="form-modal">
      <Modal.Header closeButton>
        <Modal.Title>{modalState.type === 'add' ? 'Add New User' : 'Edit User'}</Modal.Title>
      </Modal.Header>
      <Form onSubmit={handleInternalSubmit}>
        <Modal.Body>
          <Row>
              <Col md={12}>
                  <Form.Group className="mb-3">
                      <Form.Label>Full Name</Form.Label>
                      <Form.Control type="text" name="name" required autoFocus defaultValue={modalState.currentItem?.name || ''} />
                  </Form.Group>
              </Col>
              <Col md={12}>
                  <Form.Group className="mb-3">
                      <Form.Label>Email Address</Form.Label>
                      <Form.Control type="email" name="email" required defaultValue={modalState.currentItem?.email || ''} />
                  </Form.Group>
              </Col>
              <Col md={12}>
                  <Form.Group className="mb-3">
                      <Form.Label>Password</Form.Label>
                      <Form.Control type="password" name="password" placeholder={modalState.type === 'edit' ? "Leave blank to keep current password" : ""} required={modalState.type === 'add'} />
                  </Form.Group>
              </Col>
               <Col md={12}>
                  <Form.Group className="mb-3">
                      <Form.Label>Role</Form.Label>
                      <Form.Select name="role" required defaultValue={modalState.currentItem?.role || 'ROLE_USER'}>
                          {roleOptions.map(role => <option key={role} value={role}>{role.replace('ROLE_', '')}</option>)}
                      </Form.Select>
                  </Form.Group>
              </Col>
          </Row>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide}>Cancel</Button>
          <Button variant="primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? <><Spinner as="span" animation="border" size="sm" /> Saving...</> : 'Save Changes'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export default UserFormModal;