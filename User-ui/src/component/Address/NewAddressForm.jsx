

import React from 'react';
import { Form, Row, Col } from 'react-bootstrap';

/**
 * Component ฟอร์มสำหรับกรอกที่อยู่ใหม่
 * @param {object} props
 * @param {object} props.address 
 * @param {function} props.onChange 
 */
const NewAddressForm = ({ address, onChange }) => {
    
    
    const handleChange = (e) => {
        const { name, value } = e.target;
        
        onChange({
            ...address,
            [name]: value,
        });
    };

    return (
        <div className="mt-3 border p-3 rounded bg-light">
            <Row>
                <Col md={6}>
                    <Form.Group className="mb-3">
                        <Form.Label>ชื่อ-นามสกุล ผู้รับ</Form.Label>
                        <Form.Control
                            type="text"
                            name="contactName"
                            value={address.contactName}
                            onChange={handleChange}
                            required
                            placeholder="เช่น สมชาย ใจดี"
                        />
                    </Form.Group>
                </Col>
                <Col md={6}>
                    <Form.Group className="mb-3">
                        <Form.Label>เบอร์โทรศัพท์</Form.Label>
                        <Form.Control
                            type="tel"
                            name="phoneNumber"
                            value={address.phoneNumber}
                            onChange={handleChange}
                            required
                            placeholder="เช่น 0812345678"
                        />
                    </Form.Group>
                </Col>
            </Row>
            <Form.Group className="mb-3">
                <Form.Label>ที่อยู่ (บ้านเลขที่, หมู่, ซอย, ถนน)</Form.Label>
                <Form.Control
                    type="text"
                    name="line1"
                    value={address.line1}
                    onChange={handleChange}
                    required
                    placeholder="บ้านเลขที่ 123/45 หมู่ 6 ซอยพัฒนา"
                />
            </Form.Group>
            <Form.Group className="mb-3">
                <Form.Label>ที่อยู่เพิ่มเติม (ถ้ามี)</Form.Label>
                <Form.Control
                    type="text"
                    name="line2"
                    value={address.line2}
                    onChange={handleChange}
                    placeholder="เช่น อาคาร B ชั้น 7"
                />
            </Form.Group>
            <Row>
                <Col md={6}>
                    <Form.Group className="mb-3">
                        <Form.Label>แขวง/ตำบล</Form.Label>
                        <Form.Control
                            type="text"
                            name="subdistrict"
                            value={address.subdistrict}
                            onChange={handleChange}
                            required
                            placeholder="บางแค"
                        />
                    </Form.Group>
                </Col>
                <Col md={6}>
                     <Form.Group className="mb-3">
                        <Form.Label>เขต/อำเภอ</Form.Label>
                        <Form.Control
                            type="text"
                            name="district"
                            value={address.district}
                            onChange={handleChange}
                            required
                            placeholder="บางแค"
                        />
                    </Form.Group>
                </Col>
            </Row>
            <Row>
                <Col md={6}>
                    <Form.Group className="mb-3">
                        <Form.Label>จังหวัด</Form.Label>
                        <Form.Control
                            type="text"
                            name="province"
                            value={address.province}
                            onChange={handleChange}
                            required
                            placeholder="กรุงเทพมหานคร"
                        />
                    </Form.Group>
                </Col>
                <Col md={6}>
                    <Form.Group className="mb-3">
                        <Form.Label>รหัสไปรษณีย์</Form.Label>
                        <Form.Control
                            type="text"
                            name="zipCode"
                            value={address.zipCode}
                            onChange={handleChange}
                            required
                            placeholder="10160"
                        />
                    </Form.Group>
                </Col>
            </Row>
        </div>
    );
};

export default NewAddressForm;