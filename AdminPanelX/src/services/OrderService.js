// src/services/OrderService.js

const API_BASE_URL = 'http://localhost:8080/api/admin/orders';

const apiRequest = async (url, method = 'GET', body = null, token) => {
    const options = {
        method,
        headers: {
            'Authorization': `Bearer ${token}`,
        },
        // --- THE FIX IS HERE ---
        // 'no-cache' tells the browser to always revalidate with the server,
        // ensuring you always get the freshest data for GET requests.
        cache: 'no-cache', 
    };
    if (body) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `Request failed with status ${response.status}` }));
        throw new Error(errorData.message || 'An unknown error occurred.');
    }
    if (response.status === 204) {
        return true;
    }
    return response.json();
};

export const fetchAllOrders = (token) => {
    return apiRequest(`${API_BASE_URL}`, 'GET', null, token);
};

export const fetchAllOrderStatuses = (token) => {
    return apiRequest(`${API_BASE_URL}/statuses`, 'GET', null, token);
};

export const fetchOrderById = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/${orderId}`, 'GET', null, token);
};

export const approveSlip = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/approve-slip/${orderId}`, 'POST', null, token);
};

export const shipOrder = (orderId, shippingData, token) => {
    return apiRequest(`${API_BASE_URL}/ship/${orderId}`, 'POST', shippingData, token);
};

export const approveRefund = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/approve-refund/${orderId}`, 'POST', null, token);
};

export const rejectRefund = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/reject-refund/${orderId}`, 'POST', null, token);
};

export const updateOrderStatus = (orderId, newStatus, token) => {
    return apiRequest(`${API_BASE_URL}/status/${orderId}`, 'POST', { newStatus }, token);
};

export const fetchValidNextStatuses = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/next-statuses/${orderId}`, 'GET', null, token);
};

export const updateShippingDetails = (orderId, shippingData, token) => {
    return apiRequest(`${API_BASE_URL}/update-shipping/${orderId}`, 'PUT', shippingData, token);
};