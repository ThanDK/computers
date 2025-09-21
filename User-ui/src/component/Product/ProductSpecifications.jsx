

import React from 'react';
import { Card, ListGroup } from 'react-bootstrap';


const SpecItem = ({ label, value, unit = '' }) => {
    
    if (!value || (Array.isArray(value) && value.length === 0)) {
        return null;
    }
    
    
    const displayValue = Array.isArray(value) ? value.join(', ') : value;

    return (
        <ListGroup.Item className="d-flex justify-content-between align-items-start">
            <div className="ms-2 me-auto">
                <div className="fw-bold">{label}</div>
            </div>
            <span className="badge bg-secondary rounded-pill p-2">
                {displayValue}{unit}
            </span>
        </ListGroup.Item>
    );
};


const ProductSpecifications = ({ product }) => {
    
    const renderCaseSpecs = () => (
        <>
            <SpecItem label="MPN" value={product.mpn} />
            <SpecItem label="Supported Form Factors" value={product.supportedFormFactors} />
            <SpecItem label="Supported PSU Form Factors" value={product.supportedPsuFormFactors} />
            <SpecItem label="Max GPU Length" value={product.max_gpu_length_mm} unit=" mm" />
            <SpecItem label="Max CPU Cooler Height" value={product.max_cooler_height_mm} unit=" mm" />
            <SpecItem label="2.5 inch Bays" value={product.bays_2_5_inch} />
            <SpecItem label="3.5 inch Bays" value={product.bays_3_5_inch} />
            <SpecItem label="Supported Radiator Sizes" value={product.supportedRadiatorSizesMm} />
        </>
    );

    return (
        <Card>
            <Card.Header as="h5">Specifications</Card.Header>
            <ListGroup variant="flush">
                {}
                {product.type === 'case' && renderCaseSpecs()}
                {}
            </ListGroup>
        </Card>
    );
};

export default ProductSpecifications;