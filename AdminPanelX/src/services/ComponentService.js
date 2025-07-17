// src/services/ComponentService.js
import { showConfirmation, handlePromise } from './NotificationService';

const API_BASE_URL = 'http://localhost:8080/api/components';

export async function fetchAllComponents(token) {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch components.');
    return response.json();
};

export async function deleteComponent(component, token) {
    const isConfirmed = await showConfirmation(
        'Are you sure?',
        `You are about to delete "${component.name}". This cannot be undone.`
    );

    if (!isConfirmed) return false;

    const promise = fetch(`${API_BASE_URL}/${component.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    }).then(response => {
        if (!response.ok) throw new Error('Deletion failed.');
        return true;
    });

    handlePromise(promise, {
        loading: 'Deleting component...',
        success: `"${component.name}" deleted successfully.`,
        error: 'Could not delete component.'
    });

    return promise;
};

export async function updateComponentStock(componentId, quantityChange, token) {
    const promise = fetch(`${API_BASE_URL}/stock/${componentId}`, {
        method: 'PATCH',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ quantity: quantityChange })
    }).then(async response => {
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Failed to update stock');
        }
        return response.json();
    });

    handlePromise(promise, {
        loading: 'Updating stock...',
        success: 'Stock updated!',
        error: (err) => err.message,
    });

    return promise;
};

export async function createComponent(componentData, imageFile, token) {
    const formData = new FormData();
    const componentBlob = new Blob([JSON.stringify(componentData)], {
        type: 'application/json'
    });
    formData.append('request', componentBlob);

    if (imageFile) {
        formData.append('image', imageFile);
    }
    
    const response = await fetch(`${API_BASE_URL}/`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        body: formData,
    });
    
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to create component. An unknown error occurred.' }));
        throw new Error(errorData.message || `HTTP error! Status: ${response.status}`);
    }

    return response.json();
};

export async function getComponentById(id, token) {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to fetch component details.' }));
        throw new Error(errorData.message || 'Failed to fetch component details.');
    }
    return response.json();
};

export async function updateComponent(id, componentData, imageFile, removeImage, token) {
    const formData = new FormData();
    
    formData.append('request', new Blob([JSON.stringify(componentData)], {
        type: "application/json"
    }));

    if (imageFile) {
        formData.append('image', imageFile);
    }
    
    const url = `${API_BASE_URL}/${id}?removeImage=${removeImage}`;

    const response = await fetch(url, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
        },
        body: formData,
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to update component. The server returned an error.' }));
        throw new Error(errorData.message || 'Failed to update component.');
    }

    return response.json();
};