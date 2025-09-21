

import React from 'react';
import { FaShoppingCart } from 'react-icons/fa';
import { Link } from 'react-router-dom';
import './ProductCard.css';

const ProductCard = ({ product, onAddToCart, onSelect }) => {
    const placeholderImage = 'https://placehold.co/400x400/eeeeee/cccccc?text=No+Image';
    const imageUrl = product.imageUrl || placeholderImage;
    const displayPrice = product.price?.toLocaleString('th-TH') || 'ติดต่อสอบถาม';
    
    
    const productId = product._id || product.id;

    
    if (!productId) {
        console.error("Product has no valid ID (_id or id):", product);
    }
    
    const handleButtonClick = (e) => {
        e.preventDefault(); 
        if (onSelect) {
            onSelect(product);
        } else if (onAddToCart) {
            onAddToCart(product);
        }
    };

    const buttonAriaLabel = onSelect 
        ? `Select ${product.name}` 
        : `Add ${product.name} to cart`;
        
    const buttonClassName = `add-to-cart-btn ${onSelect ? 'select-mode' : ''}`.trim();

   
    if (!productId) {
       
        return (
            <div className="product-card h-100 disabled-card">
                 <img src={imageUrl} alt={product.name || 'Untitled Product'} />
                <div className="card-body">
                    <h3 className="product-title">{product.name || 'Untitled Product'}</h3>
                    <div className="price-container">
                        <span className="current-price">Invalid Product Data</span>
                    </div>
                </div>
            </div>
        );
    }


    return (
        <Link to={`/products/${productId}`} className="product-card-link">
            <div className="product-card h-100">
                <img src={imageUrl} alt={product.name || 'Untitled Product'} />
                <div className="card-body">
                    <h3 className="product-title">{product.name || 'Untitled Product'}</h3>
                    <div className="price-container">
                        <span className="current-price">฿{displayPrice}</span>
                    </div>
                </div>
                
                <button 
                    className={buttonClassName} 
                    aria-label={buttonAriaLabel}
                    onClick={handleButtonClick}
                    disabled={product.stock === 0} 
                >
                    <FaShoppingCart />
                </button>
            </div>
        </Link>
    );
};

export default ProductCard;