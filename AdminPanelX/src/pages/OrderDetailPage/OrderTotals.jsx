import React, { useMemo } from 'react';
import { Card, ListGroup } from 'react-bootstrap';

const formatCurrency = (amount, currency) => {
    const numberPart = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(amount);
    return `${currency} ${numberPart}`;
};

function OrderTotals({ order }) {
    // --- CRASH FIX ---
    // Provide default values in case `order` is not yet loaded.
    // This prevents "Cannot destructure property of null/undefined" errors.
    const { lineItems = [], totalAmount = 0, currency = '' } = order || {};

    const subtotal = useMemo(() => {
        return lineItems.reduce((acc, item) => acc + (item.unitPrice * item.quantity), 0);
    }, [lineItems]);

    // Ensure shipping is never negative
    const shippingCost = Math.max(0, totalAmount - subtotal);

    return (
        <Card className="detail-card totals-card">
            <Card.Header>Order Summary</Card.Header>
            <ListGroup variant="flush" className="total-summary-list">
                <ListGroup.Item>
                    <span className="totals-label">Subtotal</span>
                    <span className="totals-value">{formatCurrency(subtotal, currency)}</span>
                </ListGroup.Item>
                <ListGroup.Item>
                    <span className="totals-label">Shipping</span>
                    <span className="totals-value">{formatCurrency(shippingCost, currency)}</span>
                </ListGroup.Item>
                <ListGroup.Item className="grand-total">
                    <span className="totals-label">Grand Total</span>
                    <span className="totals-value">{formatCurrency(totalAmount, currency)}</span>
                </ListGroup.Item>
            </ListGroup>
        </Card>
    );
}

export default OrderTotals;