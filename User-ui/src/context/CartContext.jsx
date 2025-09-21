

import React, { createContext, useState, useEffect, useContext } from 'react';
import { useAuth } from './AuthContext';
import * as CartService from '../services/CartService';

const CartContext = createContext(null);

/**

 @returns {object}
 */
export const useCart = () => {
    const context = useContext(CartContext);
    if (context === undefined) {
        throw new Error('useCart must be used within a CartProvider');
    }
    return context;
};


export const CartProvider = ({ children }) => {
    const { user } = useAuth();
    const [cartData, setCartData] = useState(null);
    const [isLoading, setIsLoading] = useState(true); 
    const [isUpdating, setIsUpdating] = useState(false); 
    const [error, setError] = useState(null);

    
    const fetchCart = async () => {
        
        if (!user) {
            setCartData(null);
            setIsLoading(false);
            return;
        }

        setIsLoading(true);
        setError(null);
        try {
            const response = await CartService.getCart();
            setCartData(response.data);
        } catch (err) {
            console.error("Failed to fetch cart:", err);
            setError(err.message || "Could not load cart.");
           
            if (err.response?.status === 404) {
                setCartData({ items: [], subtotal: 0, cartIconCount: 0, totalProductCount: 0 });
            }
        } finally {
            setIsLoading(false);
        }
    };

    
    useEffect(() => {
        fetchCart();
    }, [user]);

    /**
     
     @param {{ productId: string, quantity: number, itemType: 'COMPONENT' | 'BUILD' }} itemData 
     */
    const addToCart = async (itemData) => {
        if (!user) {
            alert("กรุณาเข้าสู่ระบบก่อนเพิ่มสินค้าลงตะกร้า");
            throw new Error("User not logged in");
        }
        if (isUpdating) return; 

        setIsUpdating(true);
        try {
            await CartService.addItem(itemData);
            await fetchCart(); 
        } catch (err) {
            console.error("Failed to add to cart:", err.response?.data || err.message);
            
            alert(err.response?.data?.message || "ไม่สามารถเพิ่มสินค้าลงตะกร้าได้");
            throw err; 
        } finally {
            setIsUpdating(false);
        }
    };

    /**
     
     @param {string} cartItemId 
     */
    const removeFromCart = async (cartItemId) => {
        if (isUpdating) return;
        setIsUpdating(true);
        try {
            await CartService.removeItem(cartItemId);
            await fetchCart();
        } catch (err) {
            console.error("Failed to remove from cart:", err.response?.data || err.message);
            alert(err.response?.data?.message || "เกิดข้อผิดพลาดในการลบสินค้า");
        } finally {
            setIsUpdating(false);
        }
    };

    /**
     
     @param {string} cartItemId 
     @param {number} quantity
     */
    const updateQuantity = async (cartItemId, quantity) => {
        if (isUpdating) return;

       
        if (quantity <= 0) {
            await removeFromCart(cartItemId);
        } else {
            setIsUpdating(true);
            try {
                await CartService.updateItem(cartItemId, { quantity });
                await fetchCart();
            } catch (err) {
                console.error("Failed to update quantity:", err.response?.data || err.message);
                alert(err.response?.data?.message || "ไม่สามารถอัปเดตจำนวนสินค้าได้");
            } finally {
                setIsUpdating(false);
            }
        }
    };

    
const clearCart = async () => {
    if (isUpdating) return;
    setIsUpdating(true);

    
    setCartData({ items: [], subtotal: 0, cartIconCount: 0, totalProductCount: 0 });

    try {
        
        await CartService.clearUserCart();
        
        

    } catch (err) {
        console.error("Failed to clear cart on server:", err.response?.data || err.message);
        
    } finally {
        setIsUpdating(false);
    }

    
};

    
    const contextValue = {
        cartItems: cartData?.items || [],
        itemCount: cartData?.cartIconCount || 0, 
        totalProductCount: cartData?.totalProductCount || 0, 
        totalAmount: cartData?.subtotal || 0,
        isLoading, 
        isUpdating, 
        error,
        fetchCart,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
    };

    return (
        <CartContext.Provider value={contextValue}>
            {children}
        </CartContext.Provider>
    );
};