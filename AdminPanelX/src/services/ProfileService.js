const API_BASE_URL = 'http://localhost:8080/api/profile';

export async function fetchCurrentUserProfile(token) {
    const response = await fetch(`${API_BASE_URL}/me`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch user profile.');
    return response.json();
}

export async function updateUserProfile(formData, token) {
    const response = await fetch(API_BASE_URL, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
        },
        body: formData,
    });
    
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Failed to update profile.');
    }
    return response.json();
}

export async function removeProfilePicture(token) {
    const response = await fetch(`${API_BASE_URL}/picture`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Failed to remove profile picture.');
    }
    return response.json();
}