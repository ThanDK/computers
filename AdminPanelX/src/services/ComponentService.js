import { showConfirmation } from './NotificationService';

const API_BASE_URL = 'http://localhost:8080/api/components';

/**
 * ฟังก์ชัน helper กลางสำหรับส่ง request ไปยัง API
 * @param {string} url - URL ปลายทาง
 * @param {object} options - Options สำหรับ fetch (method, headers, body)
 * @returns {Promise<any>} ผลลัพธ์จาก API ในรูปแบบ JSON หรือ true ถ้าเป็น no-content
 * @throws {Error} หาก request ล้มเหลว
 */
async function apiRequest(url, options = {}) {
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
 * ดึงข้อมูล component ทั้งหมดจาก API
 * @param {string} token - JWT token สำหรับยืนยันตัวตน
 * @returns {Promise<Array>} Array ของ component object
 */
export async function fetchAllComponents(token) {
    return apiRequest(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
};

/**
 * ลบ component หนึ่งชิ้นออกจากระบบ (หลังจากยืนยัน)
 * @param {object} component - Component object ที่ต้องการลบ
 * @param {string} token - JWT token
 * @returns {Promise<boolean|object>} Promise ที่จะ resolve เป็น true/object ถ้าสำเร็จ, หรือ false ถ้าผู้ใช้ยกเลิก
 */
export async function deleteComponent(component, token) {
    const isConfirmed = await showConfirmation(
        'Are you sure?',
        `You are about to delete "${component.name}". This cannot be undone.`
    );

    if (!isConfirmed) {
        // Throw a specific error to be caught in the mutation's onError to prevent further action
        throw new Error('Deletion cancelled by user.');
    }

    return apiRequest(`${API_BASE_URL}/${component.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    });
};

/**
 * อัปเดตสต็อกของ component (เพิ่ม/ลด)
 * @param {string} componentId - ID ของ component ที่ต้องการอัปเดต
 * @param {number} quantityChange - จำนวนที่เปลี่ยนแปลง (บวกสำหรับเพิ่ม, ลบสำหรับลด)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Promise ที่จะ resolve เป็น component object ที่อัปเดตแล้ว
 */
export async function updateComponentStock(componentId, quantityChange, token) {
    return apiRequest(`${API_BASE_URL}/stock/${componentId}`, {
        method: 'PATCH',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ quantity: quantityChange })
    });
};

/**
 * สร้าง component ใหม่
 * @param {object} componentData - ข้อมูลของ component ที่จะสร้าง
 * @param {File} imageFile - ไฟล์รูปภาพ (ถ้ามี)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Component object ที่ถูกสร้างใหม่
 */
export async function createComponent(componentData, imageFile, token) {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(componentData)], { type: 'application/json' }));
    if (imageFile) {
        formData.append('image', imageFile);
    }

    return apiRequest(`${API_BASE_URL}/`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
    });
};

/**
 * ดึงข้อมูล component หนึ่งชิ้นด้วย ID
 * @param {string} id - ID ของ component
 * @param {string} token - JWT token
 * @returns {Promise<object>} Component object ที่ตรงกับ ID
 */
export async function getComponentById(id, token) {
    return apiRequest(`${API_BASE_URL}/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
};

/**
 * อัปเดตข้อมูล component หนึ่งชิ้น
 * @param {string} id - ID ของ component ที่จะอัปเดต
 * @param {object} componentData - ข้อมูลใหม่ของ component
 * @param {File} imageFile - ไฟล์รูปภาพใหม่ (ถ้ามีการเปลี่ยน)
 * @param {boolean} removeImage - ตั้งเป็น true ถ้าต้องการลบรูปภาพปัจจุบัน
 * @param {string} token - JWT token
 * @returns {Promise<object>} Component object ที่อัปเดตแล้ว
 */
export async function updateComponent(id, componentData, imageFile, removeImage, token) {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(componentData)], { type: "application/json" }));
    if (imageFile) {
        formData.append('image', imageFile);
    }

    const url = `${API_BASE_URL}/${id}?removeImage=${removeImage}`;

    return apiRequest(url, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
    });
};