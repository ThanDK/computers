import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchAllOrders, fetchAllOrderStatuses } from '../../services/OrderService';
import { format } from 'date-fns';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';
import StatusBadge from '../../components/StatusBadge/StatusBadge';

import { Alert, Spinner, Button, Form, InputGroup } from 'react-bootstrap';
import { BsSearch, BsArrowCounterclockwise } from 'react-icons/bs';

import './OrdersPage.css';

function OrdersPage() {
    const navigate = useNavigate();
    const { token } = useAuth();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [allStatuses, setAllStatuses] = useState([]);

    const [sorting, setSorting] = useState([{ id: 'createdAt', desc: true }]);
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    
    // --- THE FIX IS IN THIS FUNCTION ---
    const loadData = useCallback(async () => {
        if (!token) return;
        setLoading(true);
        setError('');
        try {
            // We add the same small, artificial delay from the ComponentsPage
            // to ensure the loading spinner is visible briefly for a consistent UX.
            const [ordersData, statusesData] = await Promise.all([
                fetchAllOrders(token),
                fetchAllOrderStatuses(token),
                new Promise(resolve => setTimeout(resolve, 20)) // <--- THE DELAY IS ADDED HERE
            ]);
            setOrders(ordersData);
            setAllStatuses(statusesData);
        } catch (err) {
            setError(err.message || "Failed to fetch data.");
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => {
        loadData();
    }, [loadData]);
    
    const handleResetFilters = () => {
        setGlobalFilter('');
        setColumnFilters([]);
        loadData(); 
    };

    const columns = useMemo(() => [
        {
            accessorKey: 'id',
            header: 'Order ID',
            meta: { width: '20%' },
            cell: info => <span className="order-id" title={info.getValue()}>{info.getValue().slice(-8)}</span>
        },
        {
            accessorKey: 'email',
            header: 'Customer',
            meta: { width: '20%' },
        },
        {
            accessorKey: 'createdAt',
            header: 'Date',
            meta: { width: '20%' },
            cell: info => format(new Date(info.getValue()), 'dd MMM yyyy, HH:mm')
        },
        {
            accessorKey: 'totalAmount',
            header: 'Total',
            meta: { width: '150px' },
            cell: info => `฿ ${Number(info.getValue()).toLocaleString(undefined, { minimumFractionDigits: 2 })}`
        },
        {
            accessorKey: 'paymentStatus',
            header: 'Payment',
            meta: { width: '150px' },
            cell: info => <StatusBadge status={info.getValue()} type="payment" />
        },
        {
            accessorKey: 'orderStatus',
            header: 'Order Status',
            meta: { width: '180px' },
            cell: info => <StatusBadge status={info.getValue()} type="order" />
        },
        {
            id: 'actions',
            header: 'Actions',
            meta: { cellClassName: 'text-center-cell', width: '150px' },
            cell: ({ row }) => (
                <div className="d-flex gap-2 justify-content-center">
                    <Button 
                        variant="outline-primary" 
                        size="sm" 
                        className="action-btn"
                        onClick={() => navigate(`/order-details/${row.original.id}`)}
                    >
                        View Details
                    </Button>
                </div>
            )
        }
    ], [navigate]);

    const statusFilterValue = columnFilters.find(f => f.id === 'orderStatus')?.value || '';

    if (loading && !orders.length) {
        return (
            <>
                <MainHeader />
                <PageHeader title="Order Management" subtitle="View, search, and manage customer orders" />
                <div className="text-center p-5"><Spinner animation="border" /></div>
            </>
        );
    }

    return (
        <>
            <MainHeader />
            <PageHeader title="Order Management" subtitle="View, search, and manage customer orders" />
            
            <div className="table-controls-container">
                <div className="filter-controls">
                    <InputGroup className="search-bar">
                        <Form.Control 
                            placeholder="Search all orders..."
                            value={globalFilter ?? ''}
                            onChange={e => setGlobalFilter(e.target.value)}
                            className="search-input"
                        />
                        <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
                    </InputGroup>

                    <Form.Select 
                        className="type-filter"
                        aria-label="Filter by order status"
                        value={statusFilterValue}
                        onChange={e => {
                            const value = e.target.value;
                            setColumnFilters(prev => 
                                prev.filter(f => f.id !== 'orderStatus').concat(value ? [{ id: 'orderStatus', value }] : [])
                            );
                        }}
                    >
                        <option value="">All Statuses</option>
                        {allStatuses.map(status => (
                            <option key={status} value={status}>
                                {status.replace(/_/g, ' ').toLowerCase()}
                            </option>
                        ))}
                    </Form.Select>
                    
                    <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
                        <BsArrowCounterclockwise /> Reset
                    </Button>
                </div>
                 <div></div>
            </div>


            {error && !orders.length ? (
                <Alert variant="danger">{error}</Alert>
            ) : (
                <ReusableTable
                    columns={columns}
                    data={orders}
                    isLoading={loading}
                    sorting={sorting}
                    setSorting={setSorting}
                    globalFilter={globalFilter}
                    setGlobalFilter={setGlobalFilter}
                    columnFilters={columnFilters}
                    setColumnFilters={setColumnFilters}
                />
            )}
        </>
    );
}

export default OrdersPage;