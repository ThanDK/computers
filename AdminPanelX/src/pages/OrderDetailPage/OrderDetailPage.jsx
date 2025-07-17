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
    const [loading, setLoading] = useState(true); // For initial page load
    const [error, setError] = useState('');
    
    // --- FIX: State for the refresh button's loading indicator ---
    const [isRefreshing, setIsRefreshing] = useState(false);

    // --- FIX: Updated data loading function with delay ---
    const loadOrderData = useCallback(async () => {
        if (!token || !orderId) return;
        
        // This controls the button's spinner, not the full page
        setIsRefreshing(true); 
        setError('');

        try {
            // Promise.all adds a small artificial delay for better UX,
            // ensuring the spinner is visible for at least 200ms.
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
            // Always turn off the refreshing state
            setIsRefreshing(false);
        }
    }, [orderId, token]);

    // This useEffect is for the very first page load
    useEffect(() => {
        const initialLoad = async () => {
            setLoading(true);
            await loadOrderData(); // This will also handle the refresh state internally
            setLoading(false);
        };
        initialLoad();
    }, [loadOrderData]); // Note: dependency is on the memoized function

    // --- FIX: Handler for the refresh button click ---
    const handleRefresh = () => {
        // Prevent multiple refresh clicks while one is in progress
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
        return null; // Should not happen if loading/error states are handled
    }

    return (
        <>
            <MainHeader />
            {/* --- FIX: Pass new props to PageHeader --- */}
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
        </>
    );
}

export default OrderDetailPage;