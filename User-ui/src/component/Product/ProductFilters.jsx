import React, { useMemo } from 'react';
import { Form, Dropdown, InputGroup, Row, Col } from 'react-bootstrap';
import { categoriesData } from './categories';
import './ProductFilters.css';

const ProductFilters = ({ products, filters, onFilterChange }) => {
    const uniqueBrands = useMemo(() => {
        const brands = new Set(products.map(p => p.brandName).filter(Boolean));
        return [...brands].sort();
    }, [products]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        onFilterChange({ ...filters, [name]: value });
    };

    const handleBrandChange = (brandName) => {
        const currentBrands = filters.brands || [];
        const newBrands = currentBrands.includes(brandName)
            ? currentBrands.filter(b => b !== brandName)
            : [...currentBrands, brandName];
        onFilterChange({ ...filters, brands: newBrands });
    };

    return (
        <div className="product-filters-bar p-3 mb-4 bg-light rounded shadow-sm">
            <Row className="g-3 align-items-center">
                <Col lg={3} md={12}>
                    <Form.Control
                        type="text"
                        name="keyword"
                        placeholder="ค้นหาด้วยชื่อ, รุ่น..."
                        value={filters.keyword || ''}
                        onChange={handleInputChange}
                    />
                </Col>

                <Col lg={3} md={6}>
                    <InputGroup className="price-input-group">
                        <InputGroup.Text>฿</InputGroup.Text>
                        <Form.Control
                            type="number"
                            name="minPrice"
                            placeholder="ราคาต่ำสุด"
                            value={filters.minPrice || ''}
                            onChange={handleInputChange}
                            min="0"
                        />
                        <InputGroup.Text>-</InputGroup.Text>
                        <Form.Control
                            type="number"
                            name="maxPrice"
                            placeholder="ราคาสูงสุด"
                            value={filters.maxPrice || ''}
                            onChange={handleInputChange}
                            min="0"
                        />
                    </InputGroup>
                </Col>

                <Col lg={2} md={6}>
                     <Dropdown>
                        <Dropdown.Toggle variant="outline-secondary" id="dropdown-brands" className="w-100">
                           ยี่ห้อ {filters.brands && filters.brands.length > 0 ? `(${filters.brands.length})` : ''}
                        </Dropdown.Toggle>
                        <Dropdown.Menu className="brand-dropdown-menu w-100">
                            {uniqueBrands.map(brand => (
                                <Form.Check
                                    key={brand}
                                    type="checkbox"
                                    id={`brand-${brand}`}
                                    label={brand}
                                    checked={(filters.brands || []).includes(brand)}
                                    onChange={() => handleBrandChange(brand)}
                                />
                            ))}
                        </Dropdown.Menu>
                    </Dropdown>
                </Col>

                <Col lg={2} md={6}>
                    <Form.Select
                        name="sortOrder"
                        value={filters.sortOrder || 'newest'}
                        onChange={handleInputChange}
                        aria-label="Sort products by"
                    >
                        <option value="newest">สินค้าใหม่ล่าสุด</option>
                        <option value="price_asc">ราคา: น้อยไปมาก</option>
                        <option value="price_desc">ราคา: มากไปน้อย</option>
                    </Form.Select>
                </Col>

                 <Col lg={2} md={6}>
                    <Form.Select
                        name="category"
                        value={filters.category || 'All'}
                        onChange={handleInputChange}
                        aria-label="Filter by category"
                    >
                        <option value="All">ทุกหมวดหมู่</option>
                        {categoriesData.map(cat => (
                            <option key={cat.slug} value={cat.slug}>{cat.name_th}</option>
                        ))}
                    </Form.Select>
                </Col>
            </Row>
        </div>
    );
};

export default ProductFilters;