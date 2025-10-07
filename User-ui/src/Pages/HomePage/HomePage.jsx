import React, { useState } from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import ExploreComponent from '../../component/Product/ExploreComponent';
import ProductFilters from '../../component/Product/ProductFilters';

const HomePage = () => {
    const [allProducts, setAllProducts] = useState([]);
    const [filters, setFilters] = useState({
        keyword: '',
        minPrice: '',
        maxPrice: '',
        brands: [],
        sortOrder: 'newest',
        category: 'All', // Default category for home page
        // Callback for ExploreComponent to pass up the full product list
        onProductsLoaded: (products) => setAllProducts(products)
    });

    const handleFilterChange = (newFilters) => {
        setFilters(prevFilters => ({ ...prevFilters, ...newFilters }));
    };

    return (
        <Container className="my-4">
            <Row className="mb-4">
                <Col>
                    <h2>เลือกชมชิ้นส่วนคอมพิวเตอร์</h2>
                    <p className="text-muted">เลือกชมชิ้นส่วนคอมพิวเตอร์คุณภาพจากเรา</p>
                    
                    <ProductFilters 
                        products={allProducts}
                        filters={filters}
                        onFilterChange={handleFilterChange} 
                    />
                </Col>
            </Row>
            <ExploreComponent filters={filters} />
        </Container>
    );
};

export default HomePage;