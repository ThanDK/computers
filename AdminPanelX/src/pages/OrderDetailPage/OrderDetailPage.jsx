import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchOrderById } from '../../services/OrderService';
import { useQuery } from '@tanstack/react-query';
import { Spinner, Alert } from 'react-bootstrap';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import OrderActions from './components/OrderActions/OrderActions';
import OrderItemsTable from './components/OrderItemsTable/OrderItemsTable';
import OrderStatusCard from './components/OrderStatusCard/OrderStatusCard';
import OrderSummary from './components/OrderSummary/OrderSummary';
import OrderTotals from './components/OrderTotals/OrderTotals';

import './OrderDetailPage.css';

function OrderDetailPage() {
    const { orderId } = useParams();
    const navigate = useNavigate();
    const { token } = useAuth();

    // ดึงข้อมูล order ด้วย tanstack query
    const {
        data: order,
        isLoading, 
        isError,
        error,
        isFetching, 
        refetch,    
    } = useQuery({
        queryKey: ['order', orderId], 
        queryFn: () => fetchOrderById(orderId, token),
        enabled: !!token && !!orderId, // สั่งให้ run query เมื่อมี token และ orderId เท่านั้น
    });

    // หน้า loading ขณะดึงข้อมูล
    if (isLoading) {
        return (
            <>
                <MainHeader />
                <PageHeader title="Loading Order..." />
                <div className="text-center p-5">
                    <Spinner animation="border" />
                </div>
            </>
        );
    }
    
    // หน้า error ถ้าดึงข้อมูลไม่ได้
    if (isError || !order) {
        return (
            <>
                <MainHeader />
                <PageHeader title="Error" subtitle="Could not load order details" />
                <Alert variant="danger" className="m-4">
                    {error?.message || "Order not found."}
                </Alert>
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
                onRefresh={refetch}
                isRefreshing={isFetching}
            />

            <div className="order-detail-layout">
                <div className="order-main-content">
                    <OrderItemsTable 
                        lineItems={order.lineItems} 
                        currency={order.currency} 
                    />
                    <OrderTotals order={order} />
                </div>

                <div className="order-sidebar-content">

                    <OrderActions order={order} />
                    <OrderStatusCard order={order} />
                    <OrderSummary order={order} />
                </div>
            </div>
        </div>
    );
}

export default OrderDetailPage;