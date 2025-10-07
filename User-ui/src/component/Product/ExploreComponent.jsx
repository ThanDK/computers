import React, { useState, useEffect, useMemo } from 'react';
import { Row, Col, Alert, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import ProductCard from './ProductCard'; 
import { fetchAllComponents } from '../../services/ComponentService'; 
import { notifySuccess } from '../../services/NotificationService';

const ExploreComponent = ({ category = 'All', filters = {}, onSelectComponent }) => {
    const { user } = useAuth();
    const { addToCart, updatingProductId } = useCart();
    const navigate = useNavigate();

    const [allProducts, setAllProducts] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    // This prop will be used by parent components (HomePage, ProductPage) to pass back the full list of products
    // so they can populate the Brand filter dropdown.
    const { onProductsLoaded } = filters;

    useEffect(() => {
        const loadProducts = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const data = await fetchAllComponents();
                setAllProducts(data);
                if (onProductsLoaded) {
                    onProductsLoaded(data);
                }
            } catch (err) {
                setError(err.message || 'An unexpected error occurred while fetching products.');
            } finally {
                setIsLoading(false);
            }
        };
        loadProducts();
    }, [onProductsLoaded]);

    const processedProducts = useMemo(() => {
        let results = [...allProducts];

        // 1. Filter by Category
        const currentCategory = filters.category || category;
        if (currentCategory && currentCategory.toLowerCase() !== 'all') {
            results = results.filter(
                product => product.type && product.type.toLowerCase() === currentCategory.toLowerCase()
            );
        }

        // 2. Filter by Keyword
        if (filters.keyword) {
            results = results.filter(product =>
                product.name.toLowerCase().includes(filters.keyword.toLowerCase())
            );
        }

        // 3. Filter by Brands
        if (filters.brands && filters.brands.length > 0) {
            results = results.filter(product =>
                filters.brands.includes(product.brandName)
            );
        }

        // 4. Filter by Price Range
        if (filters.minPrice) {
            results = results.filter(product => product.price >= parseFloat(filters.minPrice));
        }
        if (filters.maxPrice) {
            results = results.filter(product => product.price <= parseFloat(filters.maxPrice));
        }

        // 5. Master Sort Logic
        const activeProducts = results.filter(p => p.isActive);
        const inactiveProducts = results.filter(p => !p.isActive);

        switch (filters.sortOrder) {
            case 'price_asc':
                activeProducts.sort((a, b) => a.price - b.price);
                break;
            case 'price_desc':
                activeProducts.sort((a, b) => b.price - a.price);
                break;
            case 'newest':
            default:
                activeProducts.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                break;
        }

        return [...activeProducts, ...inactiveProducts];
    }, [category, filters, allProducts]);

    const handleAddToCart = async (product) => {
        if (!user) {
            alert('กรุณาเข้าสู่ระบบก่อนเพิ่มสินค้าลงตะกร้า');
            navigate('/login');
            return;
        }

        const productId = product.id || product._id;
        if (!productId) {
            alert("เกิดข้อผิดพลาด: ไม่พบรหัสสินค้า");
            return;
        }

        const itemData = { productId, quantity: 1, itemType: 'COMPONENT' };
        try {
            await addToCart(itemData);
            notifySuccess(`'${product.name}' ถูกเพิ่มลงในตะกร้าแล้ว!`);
        } catch (err) {
            console.error("Add to cart failed at component level:", err);
        }
    };

    if (isLoading) {
        return <div className="text-center my-5"><Spinner animation="border" /></div>;
    }
    
    if (error) {
        return <Alert variant="danger" className="text-center mt-4">Error: {error}</Alert>;
    }

    return (
        <>
            {processedProducts.length === 0 && !isLoading && (
                 <Alert variant="info" className="text-center mt-4">ไม่พบสินค้าที่ตรงกับเงื่อนไขการค้นหา</Alert>
            )}
            <Row xs={1} sm={2} md={3} lg={4} xl={5} className="g-4">
                {processedProducts.map(product => (
                    <Col key={product._id || product.id}>
                        <ProductCard 
                            product={product} 
                            onAddToCart={handleAddToCart}
                            onSelect={onSelectComponent}
                            isAdding={updatingProductId === (product._id || product.id)} 
                        />
                    </Col>
                ))}
            </Row>
        </>
    );
};

export default ExploreComponent;