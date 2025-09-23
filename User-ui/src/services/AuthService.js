import api from '../api/api';

export const loginUser = async (email, password) => {
  try {
    const response = await api.post('/login', { email, password });
    return response.data.token;
  } catch (error) {
    console.error("Login API error:", error.response?.data || error.message);
    throw new Error('Login failed. Please check your credentials.');
  }
};