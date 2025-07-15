import React from 'react';
import { Card, ListGroup } from 'react-bootstrap';
import StatusBadge from '../../components/StatusBadge/StatusBadge';

function OrderStatusCard({ order }) {
    return (
        <Card className="detail-card">
            <Card.Header>Current Status</Card.Header>
            <ListGroup variant="flush">
                <ListGroup.Item>
                    <span className="totals-label">Order Status</span>
                    <StatusBadge status={order.orderStatus} type="order" />
                </ListGroup.Item>
                <ListGroup.Item>
                    <span className="totals-label">Payment Status</span>
                    <StatusBadge status={order.paymentStatus} type="payment" />
                </ListGroup.Item>
            </ListGroup>
        </Card>
    );
}

export default OrderStatusCard;