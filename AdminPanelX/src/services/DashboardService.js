// src/services/DashboardService.js

import { fetchAllComponents } from './ComponentService';
import { handlePromise } from './NotificationService';

/**
 * =========================================================================
 * FUTURE BACKEND API ENDPOINT DEFINITION
 * =========================================================================
 *
 * To power this dashboard, a new backend endpoint will be required.
 *
 * Endpoint: GET /api/admin/orders
 * Query Params:
 *   - startDate (string, ISO 8601 format, e.g., '2023-01-01T00:00:00Z')
 *   - endDate   (string, ISO 8601 format, e.g., '2023-12-31T23:59:59Z')
 *
 * Expected Response Body (Array of Orders):
 * [
 *   {
 *     "id": "ord_123",
 *     "orderStatus": "COMPLETED", // e.g., 'COMPLETED', 'PROCESSING', 'PENDING_APPROVAL'
 *     "paymentStatus": "COMPLETED", // e.g., 'COMPLETED', 'PENDING'
 *     "totalAmount": 55000.50,
 *     "createdAt": "2023-10-25T10:00:00Z",
 *     "lineItems": [
 *       { "componentId": "cpu-001", "name": "Intel Core i9", "quantity": 1 },
 *       { "componentId": "gpu-002", "name": "NVIDIA RTX 4090", "quantity": 1 }
 *     ]
 *   },
 *   ... more orders
 * ]
 */

const LOW_STOCK_THRESHOLD = 5;

// Improved list of mock product names for better testing
const MOCK_PRODUCT_LIST = [
    { id: 'gpu-4090', name: 'NVIDIA RTX 4090' },
    { id: 'ssd-980', name: 'Samsung 980 Pro 2TB' },
    { id: 'case-h5', name: 'NZXT H5 Flow' },
    { id: 'cpu-13900k', name: 'Intel Core i9-13900K' },
    { id: 'ram-veng', name: 'Corsair Vengeance 32GB' },
    { id: 'cooler-kraken', name: 'NZXT Kraken 240' },
    { id: 'psu-corsair', name: 'Corsair RM850x' },
    { id: 'mobo-z790', name: 'ASUS ROG Strix Z790-E' },
    { id: 'ssd-970', name: 'Samsung 970 Evo 1TB' },
    { id: 'gpu-4070ti', name: 'NVIDIA RTX 4070 Ti' },
];

const fetchAllOrdersInRange = async (token, startDate, endDate) => {
    // REAL IMPLEMENTATION (when backend is ready):
    // const url = `${API_BASE_URL}/admin/orders?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`;
    // const response = await fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
    // if (!response.ok) throw new Error('Failed to fetch orders.');
    // return await response.json();

    // MOCK IMPLEMENTATION FOR NOW:
    return new Promise(resolve => {
        setTimeout(() => {
            const mockOrders = [];
            // Generate random orders over the last year for realistic data
            for (let i = 0; i < 250; i++) {
                const date = new Date();
                date.setDate(date.getDate() - Math.floor(Math.random() * 365));

                if (date >= startDate && date <= endDate) {
                    const product = MOCK_PRODUCT_LIST[Math.floor(Math.random() * MOCK_PRODUCT_LIST.length)];
                    mockOrders.push({
                        id: `ord_mock_${i}`,
                        orderStatus: ['COMPLETED', 'PROCESSING', 'SHIPPED', 'PENDING_APPROVAL'][Math.floor(Math.random() * 4)],
                        paymentStatus: 'COMPLETED',
                        totalAmount: Math.random() * 50000 + 500,
                        createdAt: date.toISOString(),
                        lineItems: [{ ...product, quantity: Math.ceil(Math.random() * 2) }]
                    });
                }
            }
            resolve(mockOrders);
        }, 800);
    });
};


export const fetchDashboardData = async (token, timeRange) => {
    const endDate = new Date();
    const startDate = new Date();
    if (timeRange === 'yearly') {
        startDate.setFullYear(endDate.getFullYear() - 1);
    } else { // 'monthly'
        startDate.setDate(endDate.getDate() - 30);
    }

    const promise = Promise.all([
        fetchAllComponents(token),
        fetchAllOrdersInRange(token, startDate, endDate)
    ]).then(([components, orders]) => {

        // --- 1. Calculate Stat Cards ---
        const productCount = components.length;
        const pendingOrderCount = orders.filter(o => ['PROCESSING', 'PENDING_APPROVAL', 'REFUND_REQUESTED'].includes(o.orderStatus)).length;
        const totalRevenue = orders.filter(o => o.paymentStatus === 'COMPLETED').reduce((sum, order) => sum + order.totalAmount, 0);
        const alertCount = components.filter(c => c.quantity > 0 && c.quantity < LOW_STOCK_THRESHOLD).length;

        // --- 2. Process Data for Revenue Chart ---
        const revenueData = {};
        orders.filter(o => o.paymentStatus === 'COMPLETED').forEach(order => {
            const date = new Date(order.createdAt);
            const key = timeRange === 'yearly'
                ? date.toLocaleString('default', { month: 'short', year: '2-digit' }) // 'Oct 23'
                : date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }); // 'Oct 28'

            if (!revenueData[key]) revenueData[key] = { date: key, revenue: 0 };
            revenueData[key].revenue += order.totalAmount;
        });

        // --- 3. Process Data for Top 5 Selling Chart ---
        const salesCounts = {};
        orders.forEach(order => {
            order.lineItems.forEach(item => {
                salesCounts[item.name] = (salesCounts[item.name] || 0) + item.quantity;
            });
        });
        const top5SellingData = Object.entries(salesCounts)
            .map(([name, quantitySold]) => ({ name, quantitySold }))
            .sort((a, b) => b.quantitySold - a.quantitySold)
            .slice(0, 5); // Get top 5 selling items

        return {
            stats: {
                products: productCount,
                pendingOrders: pendingOrderCount,
                totalRevenue: totalRevenue,
                alerts: alertCount,
            },
            revenueChartData: Object.values(revenueData).sort((a, b) => new Date(a.date) - new Date(b.date)),
            topSellingChartData: top5SellingData,
        };
    });

    handlePromise(promise, {
        loading: 'Refreshing dashboard data...',
        success: 'Dashboard data up to date!',
        error: 'Failed to load dashboard data.',
    });

    return promise;
};