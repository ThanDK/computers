

import api from '../api/api'; 


export const getCart = () => {
    return api.get('/cart');
};

/**

 @param {{ productId: string, quantity: number, itemType: 'COMPONENT' | 'BUILD' }} itemData
 */
export const addItem = (itemData) => {
    
    return api.post('/cart/items', itemData);
};

/**

 @param {string} cartItemId 
 @param {{ quantity: number }} updateData
 */
export const updateItem = (cartItemId, updateData) => {
    return api.put(`/cart/items/${cartItemId}`, updateData);
};

/**

  @param {string} cartItemId 
 */
export const removeItem = (cartItemId) => {
    return api.delete(`/cart/items/${cartItemId}`);
};


export const clearUserCart = () => {
    return api.delete('/cart');
};

