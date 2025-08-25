const API_BASE_URL = 'http://localhost:8080/api/admin/lookups';

/**
 * ฟังก์ชัน helper กลางสำหรับส่ง request ไปยัง API พร้อมการจัดการ error ที่ครอบคลุม
 * @param {string} url - URL ปลายทาง
 * @param {string} [method='GET'] - HTTP method
 * @param {object|FormData} [body=null] - Body ของ request
 * @param {string} token - JWT token
 * @returns {Promise<any>} ผลลัพธ์จาก API
 * @throws {Error} หาก request ล้มเหลว
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

/**
 * ดึงข้อมูล lookup ทั้งหมดที่มีในระบบ
 * @param {string} token - JWT token
 * @returns {Promise<object>} Object ที่มี lookup data ทั้งหมด
 */
export const fetchAllLookups = async (token) => {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch form lookup data.');
    return response.json();
};

/**
 * ดึงข้อมูล lookup ตามประเภทที่ระบุ
 * @param {string} type - ประเภทของ lookup (e.g., 'sockets', 'ram-types')
 * @param {string} token - JWT token
 * @returns {Promise<Array>} Array ของ lookup object
 */
export const fetchLookupsByType = (type, token) => {
    return apiRequest(`${API_BASE_URL}/${type}`, 'GET', null, token);
};

/**
 * สร้าง lookup ใหม่
 * @param {string} type - ประเภทของ lookup
 * @param {object} data - ข้อมูลที่จะสร้าง
 * @param {string} token - JWT token
 * @returns {Promise<object>} Lookup object ที่ถูกสร้างใหม่
 */
export const createLookup = (type, data, token) => {
    return apiRequest(`${API_BASE_URL}/${type}`, 'POST', data, token);
};

/**
 * อัปเดต lookup ที่มีอยู่
 * @param {string} type - ประเภทของ lookup
 * @param {string} id - ID ของ lookup ที่จะอัปเดต
 * @param {object} data - ข้อมูลใหม่
 * @param {string} token - JWT token
 * @returns {Promise<object>} Lookup object ที่อัปเดตแล้ว
 */
export const updateLookup = (type, id, data, token) => {
    return apiRequest(`${API_BASE_URL}/${type}/${id}`, 'PUT', data, token);
};

/**
 * ลบ lookup
 * @param {string} type - ประเภทของ lookup
 * @param {string} id - ID ของ lookup ที่จะลบ
 * @param {string} token - JWT token
 * @returns {Promise<boolean>} คืนค่า true ถ้าลบสำเร็จ
 */
export const deleteLookup = (type, id, token) => {
    return apiRequest(`${API_BASE_URL}/${type}/${id}`, 'DELETE', null, token);
};

/**
 * ดึงข้อมูลบริษัทขนส่งทั้งหมด
 * @param {string} token - JWT token
 * @returns {Promise<Array>} Array ของ shipping provider object
 */
export const fetchAllShippingProviders = (token) => {
    return apiRequest(`${API_BASE_URL}/shipping-providers`, 'GET', null, token);
};

/**
 * สร้างบริษัทขนส่งใหม่
 * @param {object} providerData - ข้อมูลบริษัทขนส่ง
 * @param {File} imageFile - ไฟล์รูปภาพ (ถ้ามี)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Shipping provider object ที่สร้างใหม่
 */
export const createShippingProvider = (providerData, imageFile, token) => {
    const formData = new FormData();
    formData.append('provider', new Blob([JSON.stringify(providerData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/shipping-providers`, 'POST', formData, token);
};

/**
 * อัปเดตข้อมูลบริษัทขนส่ง
 * @param {string} id - ID ของบริษัทขนส่ง
 * @param {object} providerData - ข้อมูลใหม่
 * @param {File} imageFile - ไฟล์รูปภาพใหม่ (ถ้ามี)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Shipping provider object ที่อัปเดตแล้ว
 */
export const updateShippingProvider = (id, providerData, imageFile, token) => {
    const formData = new FormData();
    formData.append('provider', new Blob([JSON.stringify(providerData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/shipping-providers/${id}`, 'PUT', formData, token);
};

/**
 * ลบบริษัทขนส่ง
 * @param {string} id - ID ของบริษัทขนส่ง
 * @param {string} token - JWT token
 * @returns {Promise<boolean>} คืนค่า true ถ้าลบสำเร็จ
 */
export const deleteShippingProvider = (id, token) => {
    return apiRequest(`${API_BASE_URL}/shipping-providers/${id}`, 'DELETE', null, token);
};

/**
 * สร้างแบรนด์ใหม่
 * @param {object} brandData - ข้อมูลแบรนด์ (e.g., { name: '...' })
 * @param {File} imageFile - ไฟล์รูปภาพ (ถ้ามี)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Brand object ที่สร้างใหม่
 */
export const createBrand = (brandData, imageFile, token) => {
    const formData = new FormData();
    formData.append('brand', new Blob([JSON.stringify(brandData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/brands`, 'POST', formData, token);
};

/**
 * อัปเดตข้อมูลแบรนด์
 * @param {string} id - ID ของแบรนด์
 * @param {object} brandData - ข้อมูลใหม่
 * @param {File} imageFile - ไฟล์รูปภาพใหม่ (ถ้ามี)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Brand object ที่อัปเดตแล้ว
 */
export const updateBrand = (id, brandData, imageFile, token) => {
    const formData = new FormData();
    formData.append('brand', new Blob([JSON.stringify(brandData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }
    return apiRequest(`${API_BASE_URL}/brands/${id}`, 'PUT', formData, token);
};

/**
 * ลบแบรนด์
 * @param {string} id - ID ของแบรนด์
 * @param {string} token - JWT token
 * @returns {Promise<boolean>} คืนค่า true ถ้าลบสำเร็จ
 */
export const deleteBrand = (id, token) => {
    return apiRequest(`${API_BASE_URL}/brands/${id}`, 'DELETE', null, token);
};