const API_BASE_URL = 'http://localhost:8080/api';


export const loginUser = async (email, password) => {
  const response = await fetch(`${API_BASE_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    
    throw new Error('Login failed. Please check your credentials.');
  }

  const data = await response.json();
 
  return data.token;
};