const API_BASE_URL = 'http://localhost:8080/api/profile';

/**
 * ดึงข้อมูลโปรไฟล์ของผู้ใช้ที่กำลังล็อกอินอยู่
 * @param {string} token - JWT token
 * @returns {Promise<object>} Object ข้อมูลโปรไฟล์ของผู้ใช้
 * @throws {Error} หากดึงข้อมูลไม่สำเร็จ
 */
export async function fetchCurrentUserProfile(token) {
    const response = await fetch(`${API_BASE_URL}/me`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch user profile.');
    return response.json();
}

/**
 * อัปเดตข้อมูลโปรไฟล์ของผู้ใช้ (ชื่อ, อีเมล, รหัสผ่าน, รูปภาพ)
 * @param {FormData} formData - FormData object ที่มี 'profileData' (JSON blob) และ 'file' (รูปภาพ)
 * @param {string} token - JWT token
 * @returns {Promise<object>} Object ข้อมูลโปรไฟล์ที่อัปเดตแล้ว
 * @throws {Error} หากอัปเดตไม่สำเร็จ
 */
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

/**
 * ลบรูปโปรไฟล์ของผู้ใช้
 * @param {string} token - JWT token
 * @returns {Promise<object>} Object ข้อมูลโปรไฟล์ที่อัปเดตแล้ว (ไม่มีรูปภาพ)
 * @throws {Error} หากลบรูปไม่สำเร็จ
 */
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