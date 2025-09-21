

import api from './api'; 


export const getProductsByCategory = async (category) => {
    try {

        const type = category.toLowerCase().replace('drives', '').replace('kits', '');
        const response = await api.get(`/components?type=${type}`);
        
        return response.data.map(product => ({
            ...product,
            id: product._id, 
        }));
    } catch (error) {
        console.error(`Error fetching products for category ${category}:`, error);
        return [];
    }
};


export const checkCompatibilityApi = async (build) => {
    const errors = [];
    const warnings = [];
    let totalWattage = 0;

    Object.values(build).forEach(item => {
        if (!item) return;
        if (Array.isArray(item)) {
            item.forEach(part => { totalWattage += (part.partDetails.wattage || 0) * part.quantity; });
        } else {
            totalWattage += item.wattage || 0;
        }
    });

    if (build.cpu && build.motherboard && build.cpu.socket !== build.motherboard.socket) {
        errors.push(`CPU Socket (${build.cpu.socket}) ไม่ตรงกับ Motherboard Socket (${build.motherboard.socket})`);
    }

    if (build.case) {
        if (build.cooler && build.cooler.height_mm > build.case.max_cooler_height_mm) {
            errors.push(`CPU Cooler สูงเกินไป (${build.cooler.height_mm}mm). เคสรองรับสูงสุด ${build.case.max_cooler_height_mm}mm.`);
        }
        if (build.gpus && build.gpus.length > 0 && build.gpus[0].partDetails.length_mm > build.case.max_gpu_length_mm) {
            errors.push(`GPU ยาวเกินไป (${build.gpus[0].partDetails.length_mm}mm). เคสรองรับสูงสุด ${build.case.max_gpu_length_mm}mm.`);
        }
    }
    
    if (totalWattage > 300 && !build.power_supply) {
        warnings.push("กำลังไฟรวมค่อนข้างสูง แนะนำให้เลือก Power Supply");
    }

    return { isCompatible: errors.length === 0, errors, warnings, totalWattage };
};


export const saveComputerBuild = async (buildRequest) => {
    console.log("Saving build to backend:", buildRequest);
    try {
        const response = await api.post('/builds', buildRequest);
        return { success: true, ...response.data }; 
    } catch (error) {
        console.error("Error saving build:", error.response?.data || error.message);
        return { success: false, error: error.response?.data || 'An unknown error occurred' };
    }
};


export const getSavedBuildsForUser = async () => {
    try {
        const response = await api.get('/builds');
        return response.data;
    } catch (error) {
        console.error("Error fetching saved builds:", error);
        return [];
    }
};


export const getBuildDetailsById = async (buildId) => {
    try {
        const response = await api.get(`/builds/${buildId}`);
        return { success: true, data: response.data };
    } catch (error) {
        console.error(`Error fetching details for build ${buildId}:`, error);
        return { success: false, error };
    }
};