const API_BASE_URL = 'http://localhost:8080/api';

/**
 * ทำการล็อกอินผู้ใช้และคืนค่า JWT token กลับมา
 * @param {string} email - อีเมลของผู้ใช้
 * @param {string} password - รหัสผ่านของผู้ใช้
 * @returns {Promise<string>} JWT token ที่ได้รับ
 * @throws {Error} หากการล็อกอินล้มเหลว
 */
export async function loginUser(email, password) {
  const response = await fetch(`${API_BASE_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    try {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Login failed. Please check your credentials.');
    } catch (e) {
        throw new Error('Login failed. Please check your credentials.');
    }
  }

  const data = await response.json();
  return data.token;
};