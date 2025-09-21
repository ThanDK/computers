import api from '../api/api.jsx'; 

/**
 * 
 * @returns {Promise<AxiosResponse<any>>} 
 */
export const fetchMyOrders = () => {
    return api.get('/orders');
};

/**
 * 
 * @param {string} orderId 
 * @returns {Promise<AxiosResponse<any>>} 
 */
export const cancelOrderByUser = (orderId) => {
    return api.post(`/orders/cancel-by-user/${orderId}`);
};

/**
 * 
 * @param {object} orderPayload 
 * @returns {Promise<AxiosResponse<any>>} 
 */
export const createOrder = (orderPayload) => {
    return api.post('/orders', orderPayload);
};

/**
 * 
 * @param {string} orderId 
 * @returns {Promise<AxiosResponse<any>>} 
 */
export const retryPaypalPayment = (orderId) => {
    return api.post(`/orders/retry-paypal/${orderId}`);
};

/**
 * 
 * 
 * @param {string} orderId 
 * @param {File} slipFile 
 * @returns {Promise<AxiosResponse<any>>} 
 */
export const submitSlip = (orderId, slipFile) => {
    const formData = new FormData();
   
    formData.append('slipImage', slipFile);

   
    return api.post(`/orders/submit-slip/${orderId}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const resubmitSlip = async (orderId, slipFile) => {
  const formData = new FormData();
  formData.append('slipImage', slipFile); // ***สำคัญ***: ชื่อ field ('slipImage') ต้องตรงกับที่ Backend คาดหวังใน Controller

  // เรียก endpoint เดิมที่ใช้ส่งสลิป
  return api.post(`/orders/submit-slip/${orderId}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};


/**
 *
 * @returns {Promise<AxiosResponse<any>>}
 */
export const getUserAddresses = () => {
    return api.get('/user/addresses');
};

/**
 * 
 * @param {string} orderId 
 * @returns {Promise<AxiosResponse<any>>} 
 */
export const getOrderById = (orderId) => {
    return api.get(`/orders/${orderId}`);
};