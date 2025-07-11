const API_BASE_URL = 'http://localhost:8080/api/lookups';

export const fetchAllLookups = async (token) => {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch form lookup data.');
    return response.json();
};