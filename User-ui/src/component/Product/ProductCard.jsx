import React from 'react';
import { FaShoppingCart, FaPlus } from 'react-icons/fa';
import { Link, useParams } from 'react-router-dom'; // REMOVED: useLocation, ADDED: useParams
import './ProductCard.css';

const ProductCard = ({ product, onAddToCart, onSelect }) => {
    const { buildId } = useParams(); // ADDED: To get buildId for navigation link
    const placeholderImage = 'https://placehold.co/400x400/eeeeee/cccccc?text=No+Image';
    const imageUrl = product.imageUrl || placeholderImage;
    const displayPrice = product.price?.toLocaleString('th-TH') || 'ติดต่อสอบถาม';
    
    const productId = product._id || product.id;

    if (!productId) {
        console.error("Product has no valid ID (_id or id):", product);
    }
    
    // CHANGED: Mode is now determined by the presence of the `onSelect` prop.
    // This is the new single source of truth for the component's mode.
    const isSelectMode = !!onSelect;
    
    // CHANGED: Construct the link with URLSearchParams for robustness
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
                    disabled={product.stock === 0} 
                >
                    {isSelectMode ? <FaPlus /> : <FaShoppingCart />}
                </button>
            </div>
        </Link>
    );
};

export default ProductCard;