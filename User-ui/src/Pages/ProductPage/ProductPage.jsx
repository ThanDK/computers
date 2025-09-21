

import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Alert } from 'react-bootstrap';
import { useParams, useNavigate } from 'react-router-dom';
import { categoriesData, getCategoryNameBySlug } from '../../component/Product/categories';
import ExploreComponent from '../../component/Product/ExploreComponent';

const ProductPage = () => {
    const { categoryName } = useParams();
    const navigate = useNavigate();

    const [activeCategory, setActiveCategory] = useState(categoryName);

    const currentCategoryDisplayName = getCategoryNameBySlug(categoryName);

    useEffect(() => {
        setActiveCategory(categoryName);
    }, [categoryName]);

   
    const handleCategoryChange = (newCategorySlug) => {
        if (newCategorySlug.toLowerCase() === 'all') {
            navigate('/');
        } else {
            
            navigate(`/products/category/${newCategorySlug.toLowerCase()}`);
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
                    <h2>สินค้าของเรา</h2>
                    <p className="text-muted">เลือกดูสินค้าคุณภาพจากหมวดหมู่ต่างๆ</p>
                    
                    <div className="category-filters">
                        <button 
                            onClick={() => handleCategoryChange('All')} 
                            className={activeCategory.toLowerCase() === 'all' ? 'active' : ''}>
                            All
                        </button>
                        {categoriesData.map(cat => (
                            <button
                                key={cat.slug}
                                onClick={() => handleCategoryChange(cat.slug)}
                                className={activeCategory.toLowerCase() === cat.slug.toLowerCase() ? 'active' : ''}
                            >
                                {cat.name}
                            </button>
                        ))}
                    </div>
                </Col>
            </Row>

            <ExploreComponent category={activeCategory} />
            
        </Container>
    );
};

export default ProductPage;