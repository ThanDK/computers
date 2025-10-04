import React from 'react';
import { Modal, Button, Form, Row, Col } from 'react-bootstrap';
import { useForm } from '../../hook/useForm';
import { addressValidation } from '../../utils/validation';

const AddressModal = ({ show, handleClose, handleSave, address }) => {
    
    const initialState = {
        contactName: '',
        phoneNumber: '',
        line1: '',
        line2: '',
        subdistrict: '',
        district: '',
        province: '',
        zipCode: '',
        country: 'Thailand', // Add country with default value
        isDefault: false,
    };

    const { formData, errors, handleChange, handleSubmit, setFormData } = useForm(
        address || initialState,
        handleSave,
        addressValidation // Pass the validation rules to the hook
    );

    React.useEffect(() => {
        if (show) {
            setFormData(address || initialState);
        }
    }, [address, show, setFormData]);

    return (
        <Modal show={show} onHide={handleClose} backdrop="static" keyboard={false} size="lg">
            <Form noValidate onSubmit={handleSubmit}>
                <Modal.Header closeButton>
                    <Modal.Title>{address ? 'แก้ไขที่อยู่' : 'เพิ่มที่อยู่ใหม่'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Row>
                        <Col md={6}>
                            <Form.Group className="mb-3" controlId="contactName">
                                <Form.Label>ชื่อ-นามสกุล ผู้รับ</Form.Label>
                                <Form.Control
                                    type="text"
                                    name="contactName"
                                    value={formData.contactName}
                                    onChange={handleChange}
                                    required
                                    isInvalid={!!errors.contactName}
                                    placeholder="เช่น สมชาย ใจดี"
                                />
                                <Form.Control.Feedback type="invalid">
                                    {errors.contactName}
                                </Form.Control.Feedback>
                            </Form.Group>
                        </Col>
                        <Col md={6}>
                            <Form.Group className="mb-3" controlId="phoneNumber">
                                <Form.Label>เบอร์โทรศัพท์</Form.Label>
                                <Form.Control
                                    type="tel"
                                    name="phoneNumber"
                                    value={formData.phoneNumber}
                                    onChange={handleChange}
                                    required
                                    isInvalid={!!errors.phoneNumber}
                                    placeholder="เช่น 0812345678"
                                />
                                <Form.Control.Feedback type="invalid">
                                    {errors.phoneNumber}
                                </Form.Control.Feedback>
                            </Form.Group>
                        </Col>
                    </Row>
                    <Form.Group className="mb-3" controlId="line1">
                        <Form.Label>ที่อยู่ (บ้านเลขที่, หมู่, ซอย, ถนน)</Form.Label>
                        <Form.Control
                            type="text"
                            name="line1"
                            value={formData.line1}
                            onChange={handleChange}
                            required
                            isInvalid={!!errors.line1}
                            placeholder="บ้านเลขที่ 123/45 หมู่ 6 ซอยพัฒนา"
                        />
                        <Form.Control.Feedback type="invalid">
                            {errors.line1}
                        </Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group className="mb-3" controlId="line2">
                        <Form.Label>ที่อยู่เพิ่มเติม (ถ้ามี)</Form.Label>
                        <Form.Control
                            type="text"
                            name="line2"
                            value={formData.line2}
                            onChange={handleChange}
                            isInvalid={!!errors.line2}
                            placeholder="เช่น อาคาร B ชั้น 7"
                        />
                        <Form.Control.Feedback type="invalid">
                            {errors.line2}
                        </Form.Control.Feedback>
                    </Form.Group>
                    <Row>
                        <Col md={6}>
                            <Form.Group className="mb-3" controlId="subdistrict">
                                <Form.Label>แขวง/ตำบล</Form.Label>
                                <Form.Control
                                    type="text"
                                    name="subdistrict"
                                    value={formData.subdistrict}
                                    onChange={handleChange}
                                    required
                                    isInvalid={!!errors.subdistrict}
                                    placeholder="บางแค"
                                />
                                <Form.Control.Feedback type="invalid">
                                    {errors.subdistrict}
                                </Form.Control.Feedback>
                            </Form.Group>
                        </Col>
                        <Col md={6}>
                             <Form.Group className="mb-3" controlId="district">
                                <Form.Label>เขต/อำเภอ</Form.Label>
                                <Form.Control
                                    type="text"
                                    name="district"
                                    value={formData.district}
                                    onChange={handleChange}
                                    required
                                    isInvalid={!!errors.district}
                                    placeholder="บางแค"
                                />
                                <Form.Control.Feedback type="invalid">
                                    {errors.district}
                                </Form.Control.Feedback>
                            </Form.Group>
                        </Col>
                    </Row>
                    <Row>
                        <Col md={6}>
                            <Form.Group className="mb-3" controlId="province">
                                <Form.Label>จังหวัด</Form.Label>
                                <Form.Control
                                    type="text"
                                    name="province"
                                    value={formData.province}
                                    onChange={handleChange}
                                    required
                                    isInvalid={!!errors.province}
                                    placeholder="กรุงเทพมหานคร"
                                />
                                <Form.Control.Feedback type="invalid">
                                    {errors.province}
                                </Form.Control.Feedback>
                            </Form.Group>
                        </Col>
                        <Col md={6}>
                            <Form.Group className="mb-3" controlId="zipCode">
                                <Form.Label>รหัสไปรษณีย์</Form.Label>
                                <Form.Control
                                    type="text"
                                    name="zipCode"
                                    value={formData.zipCode}
                                    onChange={handleChange}
                                    required
                                    isInvalid={!!errors.zipCode}
                                    placeholder="10160"
                                />
                                <Form.Control.Feedback type="invalid">
                                    {errors.zipCode}
                                </Form.Control.Feedback>
                            </Form.Group>
                        </Col>
                    </Row>
                    <Form.Group className="mb-3" controlId="isDefault">
                        <Form.Check
                            type="checkbox"
                            name="isDefault"
                            label="ตั้งเป็นที่อยู่หลัก"
                            checked={formData.isDefault}
                            onChange={handleChange}
                        />
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose}>
                        ยกเลิก
                    </Button>
                    <Button variant="primary" type="submit">
                        บันทึกที่อยู่
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

export default AddressModal;