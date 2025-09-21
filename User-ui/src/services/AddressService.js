import api from '../api/api.jsx';

/**
 
  @param {string} token 
 */
export const getUserAddresses = (token) => {
    return api.get('/user/addresses', {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
};