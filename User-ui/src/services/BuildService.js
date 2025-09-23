import api from '../api/api';

export const checkCompatibility = async (requestBody) => {
    try {
        const response = await api.post('/builds/check-compatibility', requestBody);
        return response.data;
    } catch (error) {
        console.error("Compatibility check failed:", error);
        return {
            errors: ['เกิดข้อผิดพลาดในการเชื่อมต่อกับเซิร์ฟเวอร์เพื่อตรวจสอบความเข้ากันได้'],
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
    return await api.post('/builds', request);
};

export const updateExistingBuild = async (buildId, request) => {
    return await api.put(`/builds/${buildId}`, request);
};