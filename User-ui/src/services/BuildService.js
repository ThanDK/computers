import api from '../api/api';

export const checkCompatibility = async (requestBody) => {
    try {
        const response = await api.post('/builds/check-compatibility', requestBody);
        return response.data;
    } catch (error) {
        console.error("Compatibility check failed:", error);
        return {
            errors: ['An error occurred while connecting to the server for a compatibility check.'],
            warnings: [],
            totalWattage: 0,
            isCompatible: false
        };
    }
};

export const getBuildById = async (buildId) => {
    const response = await api.get(`/builds/${buildId}`);
    return response.data;
};

export const saveNewBuild = async (request) => {
    const response = await api.post('/builds', request);
    return response.data; // Return data for consistency
};

export const updateExistingBuild = async (buildId, request) => {
    const response = await api.put(`/builds/${buildId}`, request);
    return response.data; // Return data for consistency
};

/**
 * Fetches all saved builds for the current user.
 */
export const getSavedBuilds = async () => {
    const response = await api.get('/builds');
    return response.data;
};