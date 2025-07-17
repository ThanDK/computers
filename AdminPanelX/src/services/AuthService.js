// src/services/AuthService.js

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Logs in a user and returns the JWT token.
 * @param {string} email - The user's email.
 * @param {string} password - The user's password.
 * @returns {Promise<string>} The JWT token.
 * @throws {Error} If login fails.
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
    // Attempt to parse the error message from the server, otherwise throw a generic error.
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