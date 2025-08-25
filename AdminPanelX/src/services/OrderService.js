const API_BASE_URL = 'http://localhost:8080/api/admin/orders';

/**
 * ฟังก์ชัน helper กลางสำหรับส่ง request ไปยัง API ที่เกี่ยวกับออเดอร์
 * @param {string} url - URL ปลายทาง
 * @param {string} [method='GET'] - HTTP method
 * @param {object} [body=null] - Body ของ request
 * @param {string} token - JWT token
 * @returns {Promise<any>} ผลลัพธ์จาก API
 * @throws {Error} หาก request ล้มเหลว
 */
async function apiRequest(url, method = 'GET', body = null, token) {
    const options = {
        method,
        headers: {
            'Authorization': `Bearer ${token}`,
        },
        cache: 'no-cache',
    };
    if (body) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `Request failed with status ${response.status}` }));
        throw new Error(errorData.message || 'An unknown error occurred.');
    }
    if (response.status === 204) {
        return true;
    }
    return response.json();
}

/**
 * ดึงข้อมูลออเดอร์ทั้งหมด
 * @param {string} token - JWT token
 * @returns {Promise<Array>} Array ของ order object
 */
export const fetchAllOrders = (token) => {
    return apiRequest(`${API_BASE_URL}`, 'GET', null, token);
};

/**
 * ดึงรายการสถานะออเดอร์ทั้งหมดที่เป็นไปได้
 * @param {string} token - JWT token
 * @returns {Promise<Array>} Array ของชื่อสถานะ (string)
 */
export const fetchAllOrderStatuses = (token) => {
    return apiRequest(`${API_BASE_URL}/statuses`, 'GET', null, token);
};

/**
 * ดึงข้อมูลออเดอร์หนึ่งรายการด้วย ID
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object
 */
export const fetchOrderById = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/${orderId}`, 'GET', null, token);
};

/**
 * อนุมัติสลิปการโอนเงิน
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const approveSlip = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/approve-slip/${orderId}`, 'POST', null, token);
};

/**
 * บันทึกข้อมูลการจัดส่งและเปลี่ยนสถานะออเดอร์เป็น 'SHIPPED'
 * @param {string} orderId - ID ของออเดอร์
 * @param {object} shippingData - ข้อมูลการจัดส่ง (shippingProvider, trackingNumber)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const shipOrder = (orderId, shippingData, token) => {
    return apiRequest(`${API_BASE_URL}/ship/${orderId}`, 'POST', shippingData, token);
};

/**
 * อนุมัติคำขอคืนเงิน
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const approveRefund = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/approve-refund/${orderId}`, 'POST', null, token);
};

/**
 * ปฏิเสธคำขอคืนเงิน
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const rejectRefund = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/reject-refund/${orderId}`, 'POST', null, token);
};

/**
 * อัปเดตสถานะออเดอร์ด้วยตนเอง
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} newStatus - สถานะใหม่
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const updateOrderStatus = (orderId, newStatus, token) => {
    return apiRequest(`${API_BASE_URL}/status/${orderId}`, 'POST', { newStatus }, token);
};

/**
 * ดึงรายการสถานะถัดไปที่เป็นไปได้สำหรับออเดอร์นั้นๆ
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} token - JWT token
 * @returns {Promise<Array>} Array ของชื่อสถานะ (string)
 */
export const fetchValidNextStatuses = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/next-statuses/${orderId}`, 'GET', null, token);
};

/**
 * อัปเดตรายละเอียดการจัดส่ง (เช่น แก้ไข tracking number)
 * @param {string} orderId - ID ของออเดอร์
 * @param {object} shippingData - ข้อมูลการจัดส่งใหม่
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const updateShippingDetails = (orderId, shippingData, token) => {
    return apiRequest(`${API_BASE_URL}/update-shipping/${orderId}`, 'PUT', shippingData, token);
};

/**
 * ปฏิเสธสลิปการโอนเงินพร้อมเหตุผล
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} reason - เหตุผลที่ปฏิเสธ
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const rejectSlip = (orderId, reason, token) => {
    return apiRequest(`${API_BASE_URL}/reject-slip/${orderId}`, 'POST', { reason }, token);
};

/**
 * ย้อนกลับการอนุมัติสลิป (เช่น กรณีอนุมัติผิด)
 * @param {string} orderId - ID ของออเดอร์
 * @param {string} reason - เหตุผลที่ย้อนกลับ
 * @param {string} token - JWT token
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const revertSlipApproval = (orderId, reason, token) => {
    return apiRequest(`${API_BASE_URL}/revert-approval/${orderId}`, 'POST', { reason }, token);
};

/**
 * บังคับคืนเงินออเดอร์โดยแอดมิน (เช่น สินค้ามีปัญหา)
 * @param {string} orderId - ID ของออเดอร์ที่ต้องการคืนเงิน
 * @param {string} token - JWT token ของแอดมิน
 * @returns {Promise<object>} Order object ที่อัปเดตแล้ว
 */
export const forceRefundByAdmin = (orderId, token) => {
    return apiRequest(`${API_BASE_URL}/force-refund/${orderId}`, 'POST', null, token);
};