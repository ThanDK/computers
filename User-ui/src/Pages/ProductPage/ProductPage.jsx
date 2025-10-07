import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Alert } from 'react-bootstrap';
import { useParams, useNavigate } from 'react-router-dom';
import { categoriesData, getCategoryNameBySlug } from '../../component/Product/categories';
import ExploreComponent from '../../component/Product/ExploreComponent';
import ProductFilters from '../../component/Product/ProductFilters';

const ProductPage = () => {
    const { categoryName } = useParams();
    const navigate = useNavigate();

    const [allProducts, setAllProducts] = useState([]);
    const [filters, setFilters] = useState({
        keyword: '',
        minPrice: '',
        maxPrice: '',
        brands: [],
        sortOrder: 'newest',
        // Callback for ExploreComponent to pass up the full product list
        onProductsLoaded: (products) => setAllProducts(products)
    });

    const currentCategoryDisplayName = getCategoryNameBySlug(categoryName, 'th');

    useEffect(() => {
        // Reset filters when category changes
        setFilters(prev => ({
            ...prev,
            keyword: '',
            minPrice: '',
            maxPrice: '',
            brands: [],
            sortOrder: 'newest'
        }));
    }, [categoryName]);

    const handleFilterChange = (newFilters) => {
        setFilters(prevFilters => ({ ...prevFilters, ...newFilters }));
    };

    const handleCategoryNavigation = (newCategorySlug) => {
         if (newCategorySlug.toLowerCase() === 'all') {
            navigate('/');
        } else {
            navigate(`/products/category/${newCategorySlug.toLowerCase()}`);
        }
    };
    
    // Create a modified filter object for the ProductFilters component
    const filtersForUI = {
        ...filters,
        category: categoryName, // Set category from URL
        onFilterChange: (newFilters) => {
            // If the category is changed via the dropdown, navigate instead of just setting state
            if (newFilters.category && newFilters.category !== categoryName) {
                handleCategoryNavigation(newFilters.category);
            } else {
                handleFilterChange(newFilters);
            }
        }
    };
    
    if (!currentCategoryDisplayName) {
        return (
            <Container className="my-5">
                <Alert variant="danger">ไม่พบหมวดหมู่สินค้าที่ระบุ</Alert>
            </Container>
        );
    }

    return (
        <Container className="my-5">
            <Row className="mb-4">
                <Col>
                    <h2>{currentCategoryDisplayName}</h2>
                    <p className="text-muted">เลือกดูสินค้าคุณภาพจากหมวดหมู่ต่างๆ</p>
                    
                    <ProductFilters 
                        products={allProducts}
                        filters={filtersForUI} // Pass the modified filter object
                        onFilterChange={filtersForUI.onFilterChange}
                    />
                </Col>
            </Row>

            <ExploreComponent category={categoryName} filters={filters} />
            
        </Container>
    );
};

export default ProductPage;