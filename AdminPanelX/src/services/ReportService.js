import { format } from 'date-fns';
import { API_BASE_URL } from './apiConfig';

const REPORT_ENDPOINT = `${API_BASE_URL}/reports`;
const ADMIN_ENDPOINT = `${API_BASE_URL}/admin`;

async function apiRequest(url, method = 'GET', body = null, token) {
    const options = {
        method,
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };
    if (body) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `Request failed with status ${response.status}` }));
        throw new Error(errorData.message || 'An unknown API error occurred.');
    }
    return response.json();
}

async function apiDownloadRequest(url, method = 'GET', body = null, token) {
    const options = {
        method,
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };
    if (body) {
        options.body = JSON.stringify(body);
    }
    
    const response = await fetch(url, options);

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `Download request failed with status ${response.status}` }));
        throw new Error(errorData.message || 'An unknown API error occurred during download.');
    }
    return response;
}


export const fetchAllOrderStatuses = (token) => {
    return apiRequest(`${ADMIN_ENDPOINT}/orders/statuses`, 'GET', null, token);
};

export const fetchPaymentStatuses = (token) => {
    return apiRequest(`${REPORT_ENDPOINT}/lookups/payment-statuses`, 'GET', null, token);
};

export const fetchPaymentMethods = (token) => {
    return apiRequest(`${REPORT_ENDPOINT}/lookups/payment-methods`, 'GET', null, token);
};

export const searchOrders = (token, searchRequest) => {
    const body = {
        ...searchRequest,
        startDate: searchRequest.startDate ? new Date(searchRequest.startDate).toISOString() : null,
        endDate: searchRequest.endDate ? new Date(searchRequest.endDate).toISOString() : null,
    };
    return apiRequest(`${REPORT_ENDPOINT}/orders/search`, 'POST', body, token);
};

export const fetchTopSellingReport = (token, startDate, endDate) => {
    const startQuery = format(startDate, 'yyyy-MM-dd');
    const endQuery = format(endDate, 'yyyy-MM-dd');
    const url = `${REPORT_ENDPOINT}/products/top-selling?startDate=${startQuery}&endDate=${endQuery}`;
    return apiRequest(url, 'GET', null, token);
};

export const fetchLowStockReport = (token) => {
    return apiRequest(`${REPORT_ENDPOINT}/stock/low`, 'GET', null, token);
};

export async function downloadReportAsCsv(token, url, fileName, body = null) {
    const downloadUrl = new URL(url);
    downloadUrl.searchParams.append('format', 'csv');

    const method = body ? 'POST' : 'GET';
    const response = await apiDownloadRequest(downloadUrl.toString(), method, body, token);
    
    const blob = await response.blob();
    
    const contentDisposition = response.headers.get('content-disposition');
    let finalFileName = `${fileName}-${new Date().toISOString().split('T')[0]}.csv`;

    if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
        if (filenameMatch && filenameMatch[1]) {
            finalFileName = filenameMatch[1];
        }
    }
    
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.download = finalFileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(link.href);
}