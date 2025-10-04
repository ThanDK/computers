import api from '../api/api.jsx';

/**
 * Fetches all of the current user's orders.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const fetchMyOrders = () => {
    return api.get('/orders');
};

/**
 * Submits a request from a user to cancel their own order.
 * @param {string} orderId The ID of the order to cancel.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const cancelOrderByUser = (orderId) => {
    return api.post(`/orders/cancel/${orderId}`);
};

/**
 * Creates a new order from the user's cart.
 * @param {object} orderPayload The payload containing payment and address info.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const createOrder = (orderPayload) => {
    return api.post('/orders', orderPayload);
};

/**
 * Retries a failed PayPal payment for an existing order.
 * @param {string} orderId The ID of the order to retry payment for.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const retryPaypalPayment = (orderId) => {
    return api.post(`/orders/retry-paypal/${orderId}`);
};

/**
 * Submits a payment slip for a bank transfer order.
 * @param {string} orderId The ID of the order.
 * @param {File} slipFile The image file of the payment slip.
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

/**
 * Re-submits a new payment slip for a rejected bank transfer order.
 * @param {string} orderId The ID of the order.
 * @param {File} slipFile The new image file of the payment slip.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const resubmitSlip = async (orderId, slipFile) => {
  const formData = new FormData();
  formData.append('slipImage', slipFile);

  return api.post(`/orders/submit-slip/${orderId}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

/**
 * Submits a request from a user to get a refund for an order.
 * @param {string} orderId The ID of the order to request a refund for.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const requestRefund = (orderId) => {
    return api.post(`/orders/request-refund/${orderId}`);
};

/**
 * Fetches a single order by its ID for the current user.
 * @param {string} orderId The ID of the order.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const getOrderById = (orderId) => {
    return api.get(`/orders/${orderId}`);
};

/**
 * (Public) Fetches the default payment method details for bank transfers.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const fetchDefaultPaymentMethod = () => {
    return api.get('/payment-methods/default');
};