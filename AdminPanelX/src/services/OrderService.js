// src/services/OrderService.js

const API_BASE_URL = 'http://localhost:8080/api/admin/orders';

/**
 * Generic API request helper function.
 * This is defined as a standard function to ensure it is "hoisted" and available
 * to all other functions in this file, preventing "is not a function" errors.
 */
async function apiRequest(url, method = 'GET', body = null, token) {
    const options = {
        method,
        headers: {
            'Authorization': `Bearer ${token}`,
        },
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
    // Handle successful responses with no content (e.g., DELETE)
    if (response.status === 204) {
        return true;
    }
    return response.json();
}

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

export const rejectSlip = (orderId, reason, token) => {
    return apiRequest(`${API_BASE_URL}/reject-slip/${orderId}`, 'POST', { reason }, token);
};

export const revertSlipApproval = (orderId, reason, token) => {
    return apiRequest(`${API_BASE_URL}/revert-approval/${orderId}`, 'POST', { reason }, token);
};

// --- ADD THIS NEW FUNCTION ---
/**
 * Forcibly refunds an order from the admin panel.
 * @param {string} orderId - The ID of the order to refund.
 * @param {string} token - The admin's auth token.
 * @returns {Promise<object>} - The updated order object.
 */
export const forceRefundByAdmin = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/force-refund/${orderId}`, 'POST', null, token);
};