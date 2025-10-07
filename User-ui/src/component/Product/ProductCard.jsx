import React from 'react';
import { FaShoppingCart, FaPlus } from 'react-icons/fa';
import { Link, useParams } from 'react-router-dom';
import './ProductCard.css';

const ProductCard = ({ product, onAddToCart, onSelect }) => {
    const { buildId } = useParams();
    const placeholderImage = 'https://placehold.co/400x400/eeeeee/cccccc?text=No+Image';
    const imageUrl = product.imageUrl || placeholderImage;
    const displayPrice = product.price?.toLocaleString('th-TH') || 'ติดต่อสอบถาม';
    
    const productId = product._id || product.id;

    if (!productId) {
        console.error("Product has no valid ID (_id or id):", product);
    }

    const isSelectMode = !!onSelect;
    
    let productLink = `/products/${productId}`;
    if (isSelectMode) {
        const params = new URLSearchParams();
        params.append('source', 'builder');
        if (product.type) {
            params.append('category', product.type);
        }
        if (buildId) {
            params.append('buildId', buildId);
        }
        productLink = `/products/${productId}?${params.toString()}`;
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
        
    const buttonClassName = `add-to-cart-btn ${isSelectMode ? 'select-mode' : ''}`.trim();

    // Render an inactive, unclickable card if isActive is false
    if (!product.isActive) {
        return (
            <div className="product-card h-100 inactive-card">
                <div className="out-of-stock-overlay">
                    <span>สินค้าหมด</span>
                </div>
                <img src={imageUrl} alt={product.name || 'Untitled Product'} />
                <div className="card-body">
                    <h3 className="product-title">{product.name || 'Untitled Product'}</h3>
                    <div className="price-container">
                        <span className="current-price">฿{displayPrice}</span>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <Link to={productLink} className="product-card-link">
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
                    disabled={product.quantity === 0} 
                >
                    {isSelectMode ? <FaPlus /> : <FaShoppingCart />}
                </button>
            </div>
        </Link>
    );
};

export default ProductCard;