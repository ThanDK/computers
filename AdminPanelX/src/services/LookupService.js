const API_BASE_URL = 'http://localhost:8080/api/lookups';

export const fetchAllLookups = async (token) => {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch form lookup data.');
    return response.json();
};

const apiRequest = async (url, method = 'GET', body = null, token) => {
    const options = {
        method,
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    };
    if (body) {
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);

    // For DELETE, a 204 No Content is a success with no body
    if (response.status === 204) {
        return true;
    }

    const responseData = await response.json();
    if (!response.ok) {
        throw new Error(responseData.message || `Request failed with status ${response.status}`);
    }
    return responseData;
};

/**
 * NEW: Fetches a specific type of lookup (e.g., 'sockets', 'ram-types').
 * @param {string} type - The endpoint key (e.g., 'sockets').
 * @param {string} token - The auth token.
 */
export const fetchLookupsByType = (type, token) => {
    return apiRequest(`${API_BASE_URL}/${type}`, 'GET', null, token);
};

/**
 * NEW: Creates a new lookup item.
 * @param {string} type - The endpoint key.
 * @param {object} data - The data for the new item.
 * @param {string} token - The auth token.
 */
export const createLookup = (type, data, token) => {
    return apiRequest(`${API_BASE_URL}/${type}`, 'POST', data, token);
};

/**
 * NEW: Updates an existing lookup item.
 * @param {string} type - The endpoint key.
 * @param {string} id - The ID of the item to update.
 * @param {object} data - The updated data.
 * @param {string} token - The auth token.
 */
export const updateLookup = (type, id, data, token) => {
    return apiRequest(`${API_BASE_URL}/${type}/${id}`, 'PUT', data, token);
};

/**
 * NEW: Deletes a lookup item.
 * @param {string} type - The endpoint key.
 * @param {string} id - The ID of the item to delete.
 * @param {string} token - The auth token.
 */
export const deleteLookup = (type, id, token) => {
    return apiRequest(`${API_BASE_URL}/${type}/${id}`, 'DELETE', null, token);
};