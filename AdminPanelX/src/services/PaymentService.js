import { API_BASE_URL } from './apiConfig';

const ADMIN_ENDPOINT = `${API_BASE_URL}/admin/payment-methods`;
const PUBLIC_ENDPOINT = `${API_BASE_URL}/payment-methods`;

/**
 * ฟังก์ชัน helper กลางสำหรับส่ง request ไปยัง API พร้อมการจัดการ error ที่ครอบคลุม
 * @param {string} url - URL ปลายทาง
 * @param {string} [method='GET'] - HTTP method
 * @param {object|FormData} [body=null] - Body ของ request
 * @param {string} [token=null] - JWT token (optional for public endpoints)
 * @returns {Promise<any>} ผลลัพธ์จาก API
 * @throws {Error} หาก request ล้มเหลว
 */
async function apiRequest(url, method = 'GET', body = null, token = null) {
    const options = {
        method,
        headers: {},
        cache: 'no-cache',
    };
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
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
        try {
            const errorData = await response.json();
            errorMessage = errorData.error || errorData.message || errorMessage;
        } catch (e) {
            console.error("Failed to parse JSON error response:", e);
        }
        throw new Error(errorMessage);
    }

    if (response.status === 204) {
        return true;
    }

    return response.json();
}

// --- Admin Functions ---

/**
 * (Admin) ดึงข้อมูล Payment Method ทั้งหมด
 * @param {string} token - JWT token
 * @returns {Promise<Array>} Array ของ payment method object
 */
export const fetchAllPaymentMethods = (token) => {
    return apiRequest(ADMIN_ENDPOINT, 'GET', null, token);
};

/**
 * (Admin) สร้าง Payment Method ใหม่
 * @param {object} requestData - ข้อมูล Payment Method (bankName, accountName, accountNumber)
 * @param {File} qrCodeImage - ไฟล์รูปภาพ QR Code
 * @param {string} token - JWT token
 * @returns {Promise<object>} Payment method object ที่สร้างใหม่
 */
export const createPaymentMethod = (requestData, qrCodeImage, token) => {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(requestData)], { type: 'application/json' }));
    formData.append('qrCodeImage', qrCodeImage);
    return apiRequest(ADMIN_ENDPOINT, 'POST', formData, token);
};

/**
 * (Admin) อัปเดต Payment Method
 * @param {string} id - ID ของ Payment Method
 * @param {object} requestData - ข้อมูลใหม่
 * @param {File} qrCodeImage - ไฟล์รูปภาพ QR Code ใหม่ (ถ้ามี)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Payment method object ที่อัปเดตแล้ว
 */
export const updatePaymentMethod = (id, requestData, qrCodeImage, token) => {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(requestData)], { type: 'application/json' }));
    if (qrCodeImage) {
        formData.append('qrCodeImage', qrCodeImage);
    }
    return apiRequest(`${ADMIN_ENDPOINT}/${id}`, 'PUT', formData, token);
};

/**
 * (Admin) ลบ Payment Method
 * @param {string} id - ID ของ Payment Method
 * @param {string} token - JWT token
 * @returns {Promise<boolean>} คืนค่า true ถ้าลบสำเร็จ
 */
export const deletePaymentMethod = (id, token) => {
    return apiRequest(`${ADMIN_ENDPOINT}/${id}`, 'DELETE', null, token);
};

/**
 * (Admin) ตั้งค่า Payment Method เป็น default
 * @param {string} id - ID ของ Payment Method
 * @param {string} token - JWT token
 * @returns {Promise<object>} Payment method object ที่อัปเดตแล้ว
 */
export const setDefaultPaymentMethod = (id, token) => {
    return apiRequest(`${ADMIN_ENDPOINT}/${id}/set-default`, 'POST', null, token);
};


// --- Public Functions ---

/**
 * (Public) ดึงข้อมูล Payment Method ที่เป็น default
 * @returns {Promise<object>} Default payment method object
 */
export const fetchDefaultPaymentMethod = () => {
    return apiRequest(`${PUBLIC_ENDPOINT}/default`, 'GET');
};