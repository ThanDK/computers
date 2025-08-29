import React, { useMemo } from 'react';
import { Card, ListGroup } from 'react-bootstrap';
import './OrderTotals.css';

// จัดรูปแบบตัวเลขเป็นสกุลเงิน
const formatCurrency = (amount, currency) => {
    const numericAmount = typeof amount === 'number' ? amount : 0;

    const numberPart = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(numericAmount);

    return `${currency} ${numberPart}`;
};

function OrderTotals({ order }) {
    const {
        lineItems = [],
        totalAmount = 0,
        taxAmount = 0,
        currency = ''
    } = order || {};
    
    // คำนวณยอดรวมก่อนภาษี (Subtotal)
    const subtotal = useMemo(() => {
        return lineItems.reduce((acc, item) => acc + (item.unitPrice * item.quantity), 0);
    }, [lineItems]);

    return (
        <Card className="detail-card totals-card">
            <Card.Header>Order Summary</Card.Header>

            <ListGroup variant="flush">
                <ListGroup.Item>
                    <span className="totals-label">Subtotal</span>
                    <span className="totals-value">
                        {formatCurrency(subtotal, currency)}
                    </span>
                </ListGroup.Item>

                <ListGroup.Item>
                    <span className="totals-label">VAT (7%)</span>
                    <span className="totals-value">
                        {formatCurrency(taxAmount, currency)}
                    </span>
                </ListGroup.Item>

                <ListGroup.Item className="grand-total">
                    <span className="totals-label">Grand Total</span>
                    <span className="totals-value">
                        {formatCurrency(totalAmount, currency)}
                    </span>
                </ListGroup.Item>
            </ListGroup>
        </Card>
    );
}

export default OrderTotals;