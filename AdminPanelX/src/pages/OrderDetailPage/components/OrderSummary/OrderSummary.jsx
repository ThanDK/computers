// src/pages/OrderDetailPage/components/OrderSummary/OrderSummary.jsx
import React from 'react';
import { Card, Button } from 'react-bootstrap';
import { format } from 'date-fns';
import {
    BsPerson, BsTelephone, BsGeoAlt, BsWallet2,
    BsBoxSeam, BsHash, BsCalendarPlus, BsCalendarCheck,
    BsImage, BsSlashCircle
} from 'react-icons/bs';
import './OrderSummary.css';

const DetailRow = ({ icon, label, children }) => (
    <div className="detail-row">
        <div className="detail-label">
            {icon}
            <span>{label}</span>
        </div>
        <div className="detail-value">{children}</div>
    </div>
);

function OrderSummary({ order }) {
    const {
        email, userAddress, phoneNumber, paymentDetails,
        shippingDetails, createdAt, updatedAt, orderStatus
    } = order || {};

    const getPaypalTransactionUrl = (txId) => `https://www.sandbox.paypal.com/activity/payment/${txId}`;
    const slipUrl = (paymentDetails?.paymentMethod === 'BANK_TRANSFER' && paymentDetails?.transactionId) ? paymentDetails.transactionId : null;
    const isRejected = orderStatus === 'REJECTED_SLIP';
    const buttonVariant = isRejected ? 'outline-danger' : 'outline-info';
    const buttonText = isRejected ? 'View Rejected Slip' : 'View Payment Slip';
    const buttonIcon = isRejected ? <BsSlashCircle /> : <BsImage />;

    return (
        <Card className="detail-card">
            <Card.Header>Customer & Shipping</Card.Header>
            <Card.Body className="detail-card-body">
                <DetailRow icon={<BsPerson />} label="Customer">{email}</DetailRow>
                <DetailRow icon={<BsTelephone />} label="Phone">{phoneNumber}</DetailRow>
                <div className="detail-item-full-width">
                    <div className="detail-label mb-2"><BsGeoAlt /><span>Shipping Address</span></div>
                    <p className="address-block">{userAddress}</p>
                </div>
                {shippingDetails?.shippingProvider && (<DetailRow icon={<BsBoxSeam />} label="Shipped Via">{shippingDetails.shippingProvider}</DetailRow>)}
                {shippingDetails?.trackingNumber && (<DetailRow icon={<BsHash />} label="Tracking #">{shippingDetails.trackingNumber}</DetailRow>)}
                {paymentDetails?.paymentMethod && (
                    <DetailRow icon={<BsWallet2 />} label="Paid Via">
                        {paymentDetails.paymentMethod.replace(/_/g, ' ')}
                        {paymentDetails.transactionId && paymentDetails.paymentMethod === 'PAYPAL' && (<a href={getPaypalTransactionUrl(paymentDetails.transactionId)} target="_blank" rel="noopener noreferrer" className="ms-1">(View Transaction)</a>)}
                    </DetailRow>
                )}
                <DetailRow icon={<BsCalendarPlus />} label="Created">{createdAt ? format(new Date(createdAt), 'dd MMM yyyy, HH:mm') : 'N/A'}</DetailRow>
                <DetailRow icon={<BsCalendarCheck />} label="Last Update">{updatedAt ? format(new Date(updatedAt), 'dd MMM yyyy, HH:mm') : 'N/A'}</DetailRow>

                {/* The print button has been removed. Only the slip button remains if applicable. */}
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