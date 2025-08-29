import React from 'react';
import { Card, Button } from 'react-bootstrap';
import { format } from 'date-fns';
import {
    BsPerson, BsTelephone, BsGeoAlt, BsWallet2,
    BsBoxSeam, BsHash, BsCalendarPlus, BsCalendarCheck,
    BsImage, BsSlashCircle
} from 'react-icons/bs';
import './OrderSummary.css';

// Component ย่อยสำหรับแสดงข้อมูลแต่ละแถว
const DetailRow = ({ icon, label, children }) => (
    <div className="detail-row">
        <div className="detail-label">
            {icon}
            <span>{label}</span>
        </div>
        <div className="detail-value">{children}</div>
    </div>
);

// จัดรูปแบบ object ที่อยู่เป็น string
const formatAddress = (address) => {
    if (!address) {
        return "N/A";
    }
    const { line1, line2, subdistrict, district, province, zipCode, country } = address;
    const parts = [
        line1,
        line2,
        `ต. ${subdistrict}, อ. ${district}`,
        `จ. ${province} ${zipCode}`,
        country
    ];
    return parts.filter(part => part && part.trim() !== '').join(', ');
};

function OrderSummary({ order }) {
    const {
        email,
        shippingAddress,
        paymentDetails,
        shippingDetails,
        createdAt,
        updatedAt,
        orderStatus
    } = order || {};

    // สร้าง URL สำหรับ PayPal sandbox
    const getPaypalTransactionUrl = (txId) => `https://www.sandbox.paypal.com/activity/payment/${txId}`;
    
    // หา URL ของสลิป ถ้ามี
    const slipUrl = (paymentDetails?.paymentMethod === 'BANK_TRANSFER' && paymentDetails?.slipImageUrl)
        ? paymentDetails.slipImageUrl
        : null;
    
    // กำหนดหน้าตาปุ่มดูสลิปตามสถานะ (ปกติ/ถูกปฏิเสธ)
    const isRejected = orderStatus === 'REJECTED_SLIP';
    const buttonVariant = isRejected ? 'outline-danger' : 'outline-info';
    const buttonText = isRejected ? 'View Rejected Slip' : 'View Payment Slip';
    const buttonIcon = isRejected ? <BsSlashCircle /> : <BsImage />;

    return (
        <Card className="detail-card">
            <Card.Header>Customer & Shipping</Card.Header>
            <Card.Body className="detail-card-body">

                <DetailRow icon={<BsPerson />} label="Contact Name">
                    {shippingAddress?.contactName || 'N/A'}
                </DetailRow>
                <DetailRow icon={<BsTelephone />} label="Phone">
                    {shippingAddress?.phoneNumber || 'N/A'}
                </DetailRow>
                <DetailRow icon={<BsPerson />} label="Account Email">
                    {email}
                </DetailRow>
                
                <div className="detail-item-full-width">
                    <div className="detail-label mb-2">
                        <BsGeoAlt />
                        <span>Shipping Address</span>
                    </div>
                    <p className="address-block">
                        {formatAddress(shippingAddress)}
                    </p>
                </div>

                {shippingDetails?.shippingProvider && (
                    <DetailRow icon={<BsBoxSeam />} label="Shipped Via">
                        {shippingDetails.shippingProvider}
                    </DetailRow>
                )}

                {shippingDetails?.trackingNumber && (
                    <DetailRow icon={<BsHash />} label="Tracking #">
                        {shippingDetails.trackingNumber}
                    </DetailRow>
                )}
                
                {paymentDetails?.paymentMethod && (
                    <DetailRow icon={<BsWallet2 />} label="Paid Via">
                        {paymentDetails.paymentMethod.replace(/_/g, ' ')}

                        {/* แสดงลิงก์ไป PayPal ถ้ามี transactionId */}
                        {paymentDetails.transactionId && paymentDetails.paymentMethod === 'PAYPAL' && (
                            <a
                                href={getPaypalTransactionUrl(paymentDetails.transactionId)}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="ms-1"
                            >
                                (View Transaction)
                            </a>
                        )}
                    </DetailRow>
                )}
                
                <DetailRow icon={<BsCalendarPlus />} label="Created">
                    {createdAt ? format(new Date(createdAt), 'dd MMM yyyy, HH:mm') : 'N/A'}
                </DetailRow>
                <DetailRow icon={<BsCalendarCheck />} label="Last Update">
                    {updatedAt ? format(new Date(updatedAt), 'dd MMM yyyy, HH:mm') : 'N/A'}
                </DetailRow>

                {/* แสดงปุ่มดูสลิป ถ้ามี URL */}
                {slipUrl && (
                    <div className="mt-3 d-grid">
                        <Button
                            variant={buttonVariant}
                            href={slipUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="d-flex align-items-center justify-content-center gap-2"
                        >
                            {buttonIcon} {buttonText}
                        </Button>
                    </div>
                )}
            </Card.Body>
        </Card>
    );
}

export default OrderSummary;