// src/pages/OrderDetailPage/OrderDetailPage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchOrderById } from '../../services/OrderService';
import { notifyError } from '../../services/NotificationService';
import { Spinner, Alert } from 'react-bootstrap';

// Import the page's components
import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import OrderActions from './components/OrderActions/OrderActions';
import OrderItemsTable from './components/OrderItemsTable/OrderItemsTable';
import OrderStatusCard from './components/OrderStatusCard/OrderStatusCard';
import OrderSummary from './components/OrderSummary/OrderSummary';
import OrderTotals from './components/OrderTotals/OrderTotals';

// Import the main layout CSS for this page
import './OrderDetailPage.css';

function OrderDetailPage() {
    const { orderId } = useParams();
    const navigate = useNavigate();
    const { token } = useAuth();

    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [isRefreshing, setIsRefreshing] = useState(false);

    const loadOrderData = useCallback(async () => {
        if (!token || !orderId) return;
        setIsRefreshing(true);
        setError('');
        try {
            const [data] = await Promise.all([
                fetchOrderById(orderId, token),
                new Promise(resolve => setTimeout(resolve, 200))
            ]);
            setOrder(data);
        } catch (err) {
            const errorMessage = err.message || 'Failed to refresh order details.';
            setError(errorMessage);
            notifyError(errorMessage);
        } finally {
            setIsRefreshing(false);
        }
    }, [orderId, token]);

    useEffect(() => {
        const initialLoad = async () => {
            setLoading(true);
            await loadOrderData();
            setLoading(false);
        };
        initialLoad();
    }, [loadOrderData]);

    const handleRefresh = () => {
        if (isRefreshing) return;
        loadOrderData();
    };

    if (loading) {
        return (
            <>
                <MainHeader />
                <PageHeader title="Loading Order..." />
                <div className="text-center p-5"><Spinner animation="border" /></div>
            </>
        );
    }
    
    if (error || !order) {
        return (
            <>
                <MainHeader />
                <PageHeader title="Error" subtitle="Could not load order details" />
                <Alert variant="danger" className="m-4">{error || "Order not found."}</Alert>
            </>
        );
    }

    return (
        <div className="order-detail-page-content">
            <MainHeader />
            <PageHeader
                title={`Order #${order.id.slice(-8)}`}
                subtitle={`Current Status: ${order.orderStatus.replace(/_/g, ' ')}`}
                showBackButton={true}
                onBack={() => navigate('/orders')}
                onRefresh={handleRefresh}
                isRefreshing={isRefreshing}
            />

            <div className="order-detail-layout">
                <div className="order-main-content">
                    <OrderItemsTable lineItems={order.lineItems} currency={order.currency} />
                    <OrderTotals order={order} />
                </div>
                <div className="order-sidebar-content">
                    <OrderActions 
                        key={order.updatedAt} 
                        order={order} 
                        onActionSuccess={loadOrderData} 
                    />
                    <OrderStatusCard order={order} />
                    <OrderSummary order={order} />
                </div>
            </div>
        </div>
    );
}

export default OrderDetailPage;