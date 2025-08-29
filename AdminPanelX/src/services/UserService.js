const API_BASE_URL = 'http://localhost:8080/api/admin/users';

/**
 * ดึงข้อมูลผู้ใช้ทั้งหมดจากระบบ
 * @param {string} token - JWT token
 * @returns {Promise<Array>} Array ของ user object
 * @throws {Error} หากดึงข้อมูลไม่สำเร็จ
 */
export async function fetchAllUsers(token) {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch users.');
    return response.json();
};

/**
 * สร้างผู้ใช้ใหม่โดยแอดมิน
 * @param {object} userData - ข้อมูลของผู้ใช้ที่จะสร้าง
 * @param {string} token - JWT token
 * @returns {Promise<object>} User object ที่ถูกสร้างใหม่
 * @throws {Error} หากสร้างไม่สำเร็จ หรืออีเมลซ้ำ (409 Conflict)
 */
export async function createUserByAdmin(userData, token) {
    const response = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        if (response.status === 409) {
            throw new Error(errorData.message || 'This email is already in use.');
        }
        throw new Error(errorData.message || 'Failed to create user.');
    }
    return response.json();
}

/**
 * อัปเดตข้อมูลผู้ใช้โดยแอดมิน
 * @param {string} userId - ID ของผู้ใช้ที่จะอัปเดต
 * @param {object} userData - ข้อมูลใหม่ของผู้ใช้
 * @param {string} token - JWT token
 * @returns {Promise<object>} User object ที่อัปเดตแล้ว
 * @throws {Error} หากอัปเดตไม่สำเร็จ หรืออีเมลซ้ำ (409 Conflict)
 */
export async function updateUserByAdmin(userId, userData, token) {
    const response = await fetch(`${API_BASE_URL}/${userId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        if (response.status === 409) {
            throw new Error(errorData.message || 'This email is already in use by another account.');
        }
        throw new Error(errorData.message || 'Failed to update user.');
    }
    return response.json();
}

/**
 * ลบผู้ใช้ออกจากระบบ
 * @param {object} user - User object ที่ต้องการลบ
 * @param {string} token - JWT token
 * @returns {Promise<boolean>} Promise ที่จะ resolve เป็น true ถ้าสำเร็จ
 */
export async function deleteUser(user, token) {
    const response = await fetch(`${API_BASE_URL}/${user.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!response.ok) {
        throw new Error('Deletion failed.');
    }
    return true;
};

/**
 * ล็อกบัญชีผู้ใช้
 * @param {string} userId - ID ของผู้ใช้
 * @param {string} token - JWT token
 * @returns {Promise<object>} User object ที่อัปเดตแล้ว
 * @throws {Error} หากล็อกไม่สำเร็จ
 */
export async function lockUser(userId, token) {
    const response = await fetch(`${API_BASE_URL}/lock/${userId}`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to lock user.');
    return response.json();
}

/**
 * ปลดล็อกบัญชีผู้ใช้
 * @param {string} userId - ID ของผู้ใช้
 * @param {string} token - JWT token
 * @returns {Promise<object>} User object ที่อัปเดตแล้ว
 * @throws {Error} หากปลดล็อกไม่สำเร็จ
 */
export async function unlockUser(userId, token) {
    const response = await fetch(`${API_BASE_URL}/unlock/${userId}`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to unlock user.');
    return response.json();
}