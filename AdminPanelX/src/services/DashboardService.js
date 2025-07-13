import { handlePromise } from './NotificationService';
import { format } from 'date-fns';

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Fetches all data for the main dashboard display.
 */
export const fetchDashboardData = async (token, startDate, endDate) => {
    
    const startQuery = format(startDate, 'yyyy-MM-dd');
    const endQuery = format(endDate, 'yyyy-MM-dd');
    
    const url = `${API_BASE_URL}/admin/dashboard?startDate=${startQuery}&endDate=${endQuery}`;

    const promise = fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` }
    }).then(response => {
        if (!response.ok) {
            return response.json().then(err => {
                throw new Error(err.message || 'Failed to fetch dashboard data.');
            });
        }
        return response.json();
    }).then(data => {
        return {
            stats: data.stats,
            revenueChartData: data.revenueChartData.map(item => ({
                date: new Date(item.name + 'T00:00:00Z').toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
                revenue: item.value
            })),
            topSellingData: data.topSellingData.map(item => ({
                name: item.name,
                quantitySold: item.value 
            })),
            recentOrders: data.recentOrders,
            lowStockProducts: data.lowStockProducts || [],
        };
    });

    handlePromise(promise, {
        loading: 'Refreshing dashboard data...',
        success: 'Dashboard data is up to date!',
        error: (err) => err.message || 'An unknown error occurred while loading data.',
    });

    return promise;
};


/**
 * Fetches ONLY the order data for a given date range by calling the new dedicated export endpoint.
 */
export const fetchOrdersForExport = async (token, startDate, endDate) => {
    const startQuery = format(startDate, 'yyyy-MM-dd');
    const endQuery = format(endDate, 'yyyy-MM-dd');
    
    const url = `${API_BASE_URL}/admin/dashboard/export?startDate=${startQuery}&endDate=${endQuery}`;

    const promise = fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` }
    }).then(response => {
        if (!response.ok) {
            return response.json().then(err => {
                throw new Error(err.message || 'Failed to fetch order data for export.');
            });
        }
        return response.json(); 
    });

    handlePromise(promise, {
        loading: 'Preparing export data...',
        success: 'Export data ready for download!',
        error: (err) => err.message || 'Failed to prepare export data.',
    });

    return promise;
};