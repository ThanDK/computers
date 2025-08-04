// src/services/ComponentService.js
import { showConfirmation, handlePromise } from './NotificationService';

const API_BASE_URL = 'http://localhost:8080/api/components';

async function apiRequest(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let errorMessage = `Request failed: ${response.status} ${response.statusText}`;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorData.message || errorMessage;
            } catch (e) {
                console.error("Failed to parse JSON error response:", e);
            }
        }
        throw new Error(errorMessage);
    }
    if (response.status === 204) {
        return true;
    }
    return response.json();
}

export async function fetchAllComponents(token) {
    return apiRequest(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
};

export async function deleteComponent(component, token) {
    const isConfirmed = await showConfirmation(
        'Are you sure?',
        `You are about to delete "${component.name}". This cannot be undone.`
    );

    if (!isConfirmed) return false;

    const promise = apiRequest(`${API_BASE_URL}/${component.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    });

    handlePromise(promise, {
        loading: 'Deleting component...',
        success: `"${component.name}" deleted successfully.`,
        error: (err) => err.message,
    });

    return promise;
};

export async function updateComponentStock(componentId, quantityChange, token) {
    const promise = apiRequest(`${API_BASE_URL}/stock/${componentId}`, {
        method: 'PATCH',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ quantity: quantityChange })
    });

    handlePromise(promise, {
        loading: 'Updating stock...',
        success: (updatedComponent) => `Stock for "${updatedComponent.name}" updated!`,
        error: (err) => err.message,
    });

    return promise;
};

export async function createComponent(componentData, imageFile, token) {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(componentData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }

    return apiRequest(`${API_BASE_URL}/`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
    });
};

export async function getComponentById(id, token) {
    return apiRequest(`${API_BASE_URL}/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
};

export async function updateComponent(id, componentData, imageFile, removeImage, token) {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(componentData)], { type: "application/json" }));
    if (imageFile) {
        formData.append('image', imageFile);
    }

    const url = `${API_BASE_URL}/${id}?removeImage=${removeImage}`;

    return apiRequest(url, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
    });
};