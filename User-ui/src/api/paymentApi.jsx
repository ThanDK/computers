import axios from 'axios'; 


const API_URL = '/api/payments/paypal'; 

export const createPaypalPayment = async (orderData) => {
  try {
    
    const response = await axios.post(`${API_URL}/create`, orderData, {
      headers: {
        
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`
      }
    });

    return response.data; 
  } catch (error) {
    console.error("Error creating PayPal payment:", error.response?.data || error.message);
    throw error;
  }
};


export const executePaypalPayment = async (paymentId, payerId) => {
  try {
    const response = await axios.post(`${API_URL}/execute`, { paymentId, payerId }, {
       headers: {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`
      }
    });
    
    return response.data;
  } catch (error) {
    console.error("Error executing PayPal payment:", error.response?.data || error.message);
    throw error;
  }
};