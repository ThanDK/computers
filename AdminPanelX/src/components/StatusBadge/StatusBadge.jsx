import React from 'react';
import './StatusBadge.css';

const orderStatusConfig = {
    COMPLETED: { background: '#16a34a', color: '#f0fdf4' },
    SHIPPED: { background: '#22c55e', color: '#f0fdf4' },
    PROCESSING: { background: '#2563eb', color: '#eff6ff' },
    PENDING: { background: '#f59e0b', color: '#fffbeb' }, 
    PENDING_PAYMENT: { background: '#f59e0b', color: '#fffbeb' },
    REFUND_REQUESTED: { background: '#f97316', color: '#fff7ed' },
    DELIVERY_FAILED: { background: '#dc2626', color: '#fef2f2' },
    REJECTED_SLIP: { background: '#dc2626', color: '#fef2f2' }, 
    RETURNED_TO_SENDER: { background: '#c026d3', color: '#fdf4ff' },
    CANCELLED: { background: '#64748b', color: '#f1f5f9' },
    REFUNDED: { background: '#475569', color: '#f1f5f9' },
    REFUND_REJECTED: { background: '#713f12', color: '#fefce8' }, 
    DEFAULT: { background: '#64748b', color: '#f8fafc' }
};

const paymentStatusConfig = {
    COMPLETED: { background: '#16a34a', color: '#f0fdf4' }, 
    PENDING: { background: '#f59e0b', color: '#fffbeb' }, 
    PENDING_APPROVAL: { background: '#f97316', color: '#fff7ed' }, 
    FAILED: { background: '#dc2626', color: '#fef2f2' }, 
    REJECTED: { background: '#dc2626', color: '#fef2f2' }, 
    REFUNDED: { background: '#475569', color: '#f1f5f9' }, 
    DEFAULT: { background: '#64748b', color: '#f8fafc' }
};

function StatusBadge({ status, type = 'order' }) {
    const config = type === 'payment' ? paymentStatusConfig : orderStatusConfig;
    const { background, color } = config[status] || config.DEFAULT;

    const formattedStatus = status ? status.replace(/_/g, ' ').toLowerCase() : 'N/A';

    return (
        <span className="status-badge" style={{ backgroundColor: background, color: color }}>
            {formattedStatus}
        </span>
    );
}

export default StatusBadge;