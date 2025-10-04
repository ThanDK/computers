import api from '../api/api.jsx';

/**
 * Fetches all addresses for the current user.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const getUserAddresses = () => {
    return api.get('/user/addresses');
};

/**
 * Creates a new address for the current user.
 * @param {object} addressData The new address data.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const createAddress = (addressData) => {
    return api.post('/user/addresses', addressData);
};

/**
 * Updates an existing address.
 * @param {string} addressId The ID of the address to update.
 * @param {object} addressData The updated address data.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const updateAddress = (addressId, addressData) => {
    return api.put(`/user/addresses/${addressId}`, addressData);
};

/**
 * Deletes an address.
 * @param {string} addressId The ID of the address to delete.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const deleteAddress = (addressId) => {
    return api.delete(`/user/addresses/${addressId}`);
};

/**
 * Sets a specific address as the user's default.
 * @param {string} addressId The ID of the address to set as default.
 * @returns {Promise<AxiosResponse<any>>}
 */
export const setDefaultAddress = (addressId) => {
    return api.post(`/user/addresses/set-default/${addressId}`);
};