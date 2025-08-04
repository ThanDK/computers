// src/services/UserService.js

import { showConfirmation, handlePromise } from './NotificationService';

const API_BASE_URL = 'http://localhost:8080/api/admin/users';

export async function fetchAllUsers(token) {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch users.');
    return response.json();
};

/**
 * REFACTORED: Now specifically handles 409 Conflict errors from the backend.
 */
export async function createUserByAdmin(userData, token) {
    const response = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    });

    if (!response.ok) {
        // Try to parse the JSON body of the error response.
        const errorData = await response.json().catch(() => ({}));

        // If the error is a 409 Conflict (e.g., email exists), throw the specific message.
        if (response.status === 409) {
            throw new Error(errorData.message || 'This email is already in use.');
        }

        // For all other errors, throw a generic message.
        throw new Error(errorData.message || 'Failed to create user.');
    }
    return response.json();
}

/**
 * REFACTORED: Now specifically handles 409 Conflict errors from the backend.
 */
export async function updateUserByAdmin(userId, userData, token) {
    const response = await fetch(`${API_BASE_URL}/${userId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    });

    if (!response.ok) {
        // Try to parse the JSON body of the error response.
        const errorData = await response.json().catch(() => ({}));

        // If the error is a 409 Conflict (e.g., email exists), throw the specific message.
        if (response.status === 409) {
            throw new Error(errorData.message || 'This email is already in use by another account.');
        }

        // For all other errors, throw a generic message.
        throw new Error(errorData.message || 'Failed to update user.');
    }
    return response.json();
}

export async function deleteUser(user, token) {
    const isConfirmed = await showConfirmation(
        'Are you sure?',
        `You are about to delete user "${user.name}" (${user.email}). This is permanent.`
    );
    if (!isConfirmed) return false;

    const promise = fetch(`${API_BASE_URL}/${user.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    }).then(response => {
        if (!response.ok) throw new Error('Deletion failed.');
        return true;
    });

    handlePromise(promise, {
        loading: 'Deleting user...',
        success: `User "${user.name}" deleted successfully.`,
        error: 'Could not delete user.'
    });

    return promise;
};

export async function lockUser(userId, token) {
    const response = await fetch(`${API_BASE_URL}/lock/${userId}`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to lock user.');
    return response.json();
}

export async function unlockUser(userId, token) {
    const response = await fetch(`${API_BASE_URL}/unlock/${userId}`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to unlock user.');
    return response.json();
}