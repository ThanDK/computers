import React from 'react';
import { Card } from 'react-bootstrap';
import { format } from 'date-fns';
import { 
    BsPerson, BsTelephone, BsGeoAlt, BsWallet2, 
    BsBoxSeam, BsHash, BsCalendarPlus, BsCalendarCheck 
} from 'react-icons/bs';

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
    const { email, userAddress, phoneNumber, paymentDetails, shippingDetails, createdAt, updatedAt } = order;
    
    const getPaypalTransactionUrl = (txId) => `https://www.paypal.com/activity/payment/${txId}`;

    return (
        <Card className="detail-card">
            <Card.Header>Customer & Shipping</Card.Header>
            <Card.Body className="detail-card-body">
                <DetailRow icon={<BsPerson />} label="Customer">{email}</DetailRow>
                <DetailRow icon={<BsTelephone />} label="Phone">{phoneNumber}</DetailRow>
                
                <div className="detail-item-full-width">
                    <div className="detail-label mb-2">
                        <BsGeoAlt />
                        <span>Shipping Address</span>
                    </div>
                    <p className="address-block">{userAddress}</p>
                </div>

                {shippingDetails?.shippingProvider && (
                     <DetailRow icon={<BsBoxSeam />} label="Shipped Via">{shippingDetails.shippingProvider}</DetailRow>
                )}
                {shippingDetails?.trackingNumber && (
                     <DetailRow icon={<BsHash />} label="Tracking #">{shippingDetails.trackingNumber}</DetailRow>
                )}
                {paymentDetails?.paymentMethod && (
                    <DetailRow icon={<BsWallet2 />} label="Paid Via">
                        {paymentDetails.paymentMethod.replace('_', ' ')}
                        {paymentDetails.transactionId && (
                             <a 
                                href={paymentDetails.paymentMethod === 'PAYPAL' ? getPaypalTransactionUrl(paymentDetails.transactionId) : paymentDetails.transactionId}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="transaction-link"
                            >
                                (View)
                            </a>
                        )}
                    </DetailRow>
                )}
                 <DetailRow icon={<BsCalendarPlus />} label="Created">{format(new Date(createdAt), 'dd MMM yyyy, HH:mm')}</DetailRow>
                <DetailRow icon={<BsCalendarCheck />} label="Last Update">{format(new Date(updatedAt), 'dd MMM yyyy, HH:mm')}</DetailRow>
            </Card.Body>
        </Card>
    );
}

export default OrderSummary;