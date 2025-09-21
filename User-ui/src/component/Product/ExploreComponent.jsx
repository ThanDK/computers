import React, { useState, useEffect } from 'react';
import { Row, Col, Alert, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import ProductCard from './ProductCard'; 
import { fetchAllComponents } from '../../services/ComponentService'; 
import { notifySuccess } from '../../services/NotificationService';

const ExploreComponent = ({ category = 'All', searchQuery = '', onProductSelect }) => {
    const { user } = useAuth();
    const { addToCart, isUpdating } = useCart();
    const navigate = useNavigate();

    const [allProducts, setAllProducts] = useState([]);
    const [filteredProducts, setFilteredProducts] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const loadProducts = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const data = await fetchAllComponents();
                setAllProducts(data);
            } catch (err) {
                setError(err.message || 'An unexpected error occurred while fetching products.');
            } finally {
                setIsLoading(false);
            }
        };
        loadProducts();
    }, []);

    
    useEffect(() => {
        let results = [...allProducts];
        if (category && category.toLowerCase() !== 'all') {
            results = results.filter(
                product => product.type && product.type.toLowerCase() === category.toLowerCase()
            );
        }
        if (searchQuery) {
            results = results.filter(product =>
                product.name.toLowerCase().includes(searchQuery.toLowerCase())
            );
        }
        setFilteredProducts(results);
    }, [category, searchQuery, allProducts]); 

    /**
     * จัดการการเพิ่มสินค้าลงตะกร้า
     * @param {object} product - object สินค้าที่ต้องการเพิ่ม
     */
    const handleAddToCart = async (product) => {
        if (!user) {
            alert('กรุณาเข้าสู่ระบบก่อนเพิ่มสินค้าลงตะกร้า');
            navigate('/login');
            return;
        }

        const productId = product.id || product._id;
        if (!productId) {
            alert("เกิดข้อผิดพลาด: ไม่พบรหัสสินค้า");
            console.error("Product is missing a valid ID:", product);
            return;
        }

        const itemData = {
            productId: productId,
            quantity: 1,
            itemType: 'COMPONENT'
        };

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
            {filteredProducts.length === 0 && !isLoading && (
                 <Alert variant="info" className="text-center mt-4">ไม่พบสินค้าที่ตรงกับเงื่อนไขการค้นหา</Alert>
            )}
            <Row xs={1} sm={2} md={3} lg={4} xl={5} className="g-4">
                {filteredProducts.map(product => (
                    <Col key={product._id || product.id}>
                        <ProductCard 
                            product={product} 
                            onAddToCart={handleAddToCart}
                            onSelect={onProductSelect}
                            
                            isAdding={isUpdating} 
                        />
                    </Col>
                ))}
            </Row>
        </>
    );
};

export default ExploreComponent;