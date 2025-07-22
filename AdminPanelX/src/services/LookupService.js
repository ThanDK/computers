// src/services/LookupService.js

const API_BASE_URL = 'http://localhost:8080/api/admin/lookups';

/**
 * A robust, shared API request helper defined as a standard function.
 * This ensures it is "hoisted" and fully available to all other functions in this file,
 * preventing intermittent crashes related to module load order.
 * It automatically handles JSON and FormData, authentication, and error parsing.
 * @param {string} url - The endpoint URL.
 * @param {string} method - The HTTP method (GET, POST, etc.).
 * @param {object|FormData} body - The request body.
 * @param {string} token - The JWT token.
 * @returns {Promise<any>} The response data.
 */
async function apiRequest(url, method = 'GET', body = null, token) {
    const options = {
        method,
        headers: { 'Authorization': `Bearer ${token}` },
        cache: 'no-cache',
    };
    if (body) {
        if (body instanceof FormData) {
            options.body = body;
        } else {
             options.headers['Content-Type'] = 'application/json';
             options.body = JSON.stringify(body);
        }
    }
    const response = await fetch(url, options);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `Request failed with status ${response.status}` }));
        throw new Error(errorData.message || 'An unknown error occurred.');
    }
    if (response.status === 204) return true; // For DELETE requests
    return response.json();
}

export const fetchAllLookups = async (token) => {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch form lookup data.');
    return response.json();
};

// --- Generic Lookup Functions ---

export const fetchLookupsByType = (type, token) => {
    return apiRequest(`${API_BASE_URL}/${type}`, 'GET', null, token);
};

export const createLookup = (type, data, token) => {
    return apiRequest(`${API_BASE_URL}/${type}`, 'POST', data, token);
};

export const updateLookup = (type, id, data, token) => {
    return apiRequest(`${API_BASE_URL}/${type}/${id}`, 'PUT', data, token);
};

export const deleteLookup = (type, id, token) => {
    return apiRequest(`${API_BASE_URL}/${type}/${id}`, 'DELETE', null, token);
};

// --- Specific functions for Shipping Providers ---

export const fetchAllShippingProviders = (token) => {
    return apiRequest(`${API_BASE_URL}/shipping-providers`, 'GET', null, token);
};

export const createShippingProvider = (providerData, imageFile, token) => {
    const formData = new FormData();
    formData.append('provider', new Blob([JSON.stringify(providerData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/shipping-providers`, 'POST', formData, token);
};

export const updateShippingProvider = (id, providerData, imageFile, token) => {
    const formData = new FormData();
    formData.append('provider', new Blob([JSON.stringify(providerData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/shipping-providers/${id}`, 'PUT', formData, token);
};

export const deleteShippingProvider = (id, token) => {
    return apiRequest(`${API_BASE_URL}/shipping-providers/${id}`, 'DELETE', null, token);
};

// --- ADDED: Specific functions for Brands ---

export const createBrand = (brandData, imageFile, token) => {
    const formData = new FormData();
    // The key 'brand' must match the @RequestPart("brand") in your Spring Boot controller
    formData.append('brand', new Blob([JSON.stringify(brandData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/brands`, 'POST', formData, token);
};

export const updateBrand = (id, brandData, imageFile, token) => {
    const formData = new FormData();
    // The key 'brand' must match the @RequestPart("brand") in your Spring Boot controller
    formData.append('brand', new Blob([JSON.stringify(brandData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/brands/${id}`, 'PUT', formData, token);
};