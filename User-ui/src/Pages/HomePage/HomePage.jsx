import React, { useState } from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import ExploreComponent from '../../component/Product/ExploreComponent';
import { categoriesData } from '../../component/Product/categories';
categoriesData

const HomePage = () => {
    const [activeCategory, setActiveCategory] = useState('All'); 

    return (
        <Container className="my-4">
            <Row className="mb-4">
                <Col>
                    <h2>Crafted with excellent material</h2>
                    <p className="text-muted">Browse our collection of high-quality components.</p>
                    <div className="category-filters">
                        <button 
                            onClick={() => setActiveCategory('All')} 
                            className={activeCategory === 'All' ? 'active' : ''}>
                            All
                        </button>
                        {}
                        {categoriesData.map((cat) => (
                            <button
                                key={cat.slug}
                                onClick={() => setActiveCategory(cat.slug)}
                                className={activeCategory === cat.slug ? 'active' : ''}
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

export default HomePage;