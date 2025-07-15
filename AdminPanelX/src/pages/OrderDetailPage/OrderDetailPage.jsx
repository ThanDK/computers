import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchOrderById } from '../../services/OrderService';
import { notifyError } from '../../services/NotificationService';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import OrderSummary from './OrderSummary';
import OrderItemsTable from './OrderItemsTable';
import OrderActions from './OrderActions';
import OrderTotals from './OrderTotals';
import OrderStatusCard from './OrderStatusCard';

import { Spinner, Alert } from 'react-bootstrap';
import './OrderDetailPage.css';

function OrderDetailPage() {
    const { orderId } = useParams();
    const navigate = useNavigate();
    const { token } = useAuth();

    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const loadOrderData = useCallback(async () => {
        if (!token || !orderId) return;
        setError('');
        try {
            const data = await fetchOrderById(orderId, token);
            setOrder(data);
        } catch (err) {
            setError(err.message || 'Failed to refresh order details.');
            notifyError(err.message || 'Failed to refresh order details.');
        }
    }, [orderId, token]);

    useEffect(() => {
        setLoading(true);
        loadOrderData().finally(() => setLoading(false));
    }, [loadOrderData]);


    if (loading) {
        return (
            <>
                <MainHeader />
                <PageHeader title="Loading Order..." />
                <div className="text-center p-5"><Spinner animation="border" /></div>
            </>
        );
    }

    if (error) {
        return (
            <>
                <MainHeader />
                <PageHeader title="Error" subtitle="Could not load order details" />
                <Alert variant="danger" className="m-4">{error}</Alert>
            </>
        );
    }

    if (!order) {
        return null;
    }

    return (
        <>
            <MainHeader />
            <PageHeader
                title={`Order #${order.id.slice(-8)}`}
                subtitle={`Current Status: ${order.orderStatus.replace(/_/g, ' ')}`}
                showBackButton={true}
                onBack={() => navigate('/orders')}
            />

            <div className="order-detail-layout">
                <div className="order-main-content">
                    <OrderItemsTable lineItems={order.lineItems} currency={order.currency} />
                    <OrderTotals order={order} />
                </div>
                <div className="order-sidebar-content">
                    <OrderActions order={order} onActionSuccess={loadOrderData} />
                    <OrderStatusCard order={order} />
                    <OrderSummary order={order} />
                </div>
            </div>
        </>
    );
}

export default OrderDetailPage;