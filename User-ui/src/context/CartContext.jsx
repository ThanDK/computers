import React, { createContext, useState, useEffect, useContext, useCallback } from 'react';
import { useAuth } from './AuthContext';
import * as CartService from '../services/CartService';

const CartContext = createContext(null);

/**
 * Custom hook to use the CartContext.
 * @returns {object} The cart context value.
 */
export const useCart = () => {
    // CORRECTED: The context being used must be CartContext.
    const context = useContext(CartContext);
    if (context === undefined) {
        throw new Error('useCart must be used within a CartProvider');
    }
    return context;
};

export const CartProvider = ({ children }) => {
    const { user } = useAuth();
    const [cartData, setCartData] = useState(null);
    const [isLoading, setIsLoading] = useState(true); // For initial cart load ONLY
    const [updatingProductId, setUpdatingProductId] = useState(null); // Tracks ID of product being added/updated/removed
    const [error, setError] = useState(null);

    const fetchCart = useCallback(async () => {
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
    }, [user]);

    useEffect(() => {
        fetchCart();
    }, [user, fetchCart]);

    /**
     * Adds an item to the cart.
     * @param {{ productId: string, quantity: number, itemType: 'COMPONENT' | 'BUILD' }} itemData
     */
    const addToCart = async (itemData) => {
        if (!user) {
            alert("กรุณาเข้าสู่ระบบก่อนเพิ่มสินค้าลงตะกร้า");
            throw new Error("User not logged in");
        }
        if (updatingProductId) return;

        setUpdatingProductId(itemData.productId);
        try {
            const response = await CartService.addItem(itemData);
            setCartData(response.data);
        } catch (err) {
            console.error("Failed to add to cart:", err.response?.data || err.message);
            alert(err.response?.data?.message || "ไม่สามารถเพิ่มสินค้าลงตะกร้าได้");
            throw err;
        } finally {
            setUpdatingProductId(null);
        }
    };

    /**
     * Removes an item from the cart.
     * @param {string} cartItemId
     */
    const removeFromCart = async (cartItemId) => {
        if (updatingProductId) return;
        
        const item = cartData?.items.find(i => i.cartItemId === cartItemId);
        if (item) {
            setUpdatingProductId(item.productId);
        }

        try {
            const response = await CartService.removeItem(cartItemId);
            setCartData(response.data);
        } catch (err) {
            console.error("Failed to remove from cart:", err.response?.data || err.message);
            alert(err.response?.data?.message || "เกิดข้อผิดพลาดในการลบสินค้า");
        } finally {
            setUpdatingProductId(null);
        }
    };

    /**
     * Updates the quantity of a cart item.
     * @param {string} cartItemId
     * @param {number} quantity
     */
    const updateQuantity = async (cartItemId, quantity) => {
        if (updatingProductId) return;

        if (quantity <= 0) {
            await removeFromCart(cartItemId);
        } else {
            const item = cartData?.items.find(i => i.cartItemId === cartItemId);
            if (item) {
                setUpdatingProductId(item.productId);
            }

            try {
                const response = await CartService.updateItem(cartItemId, { quantity });
                setCartData(response.data);
            } catch (err) {
                console.error("Failed to update quantity:", err.response?.data || err.message);
                alert(err.response?.data?.message || "ไม่สามารถอัปเดตจำนวนสินค้าได้");
            } finally {
                setUpdatingProductId(null);
            }
        }
    };

    const clearCart = async () => {
        try {
            const response = await CartService.clearUserCart();
            setCartData(response.data);
        } catch (err) {
            console.error("Failed to clear cart on server:", err.response?.data || err.message);
            await fetchCart();
        }
    };

    const contextValue = {
        cartItems: cartData?.items || [],
        itemCount: cartData?.cartIconCount || 0,
        totalProductCount: cartData?.totalProductCount || 0,
        totalAmount: cartData?.subtotal || 0,
        isLoading,
        updatingProductId,
        error,
        fetchCart,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
    };

    return (
        // CORRECTED: The Provider must be from CartContext.
        <CartContext.Provider value={contextValue}>
            {children}
        </CartContext.Provider>
    );
};