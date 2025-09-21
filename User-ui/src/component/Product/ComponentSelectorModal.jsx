
import React, { useState } from 'react';
import { Modal, Form, InputGroup } from 'react-bootstrap';
import ExploreComponent from './ExploreComponent'; 

const ComponentSelectorModal = ({ show, onHide, category, onSelect }) => {
    const [searchQuery, setSearchQuery] = useState('');

    const handleSelect = (product) => {
        onSelect(product);
        onHide(); 
    };
    
    return (
        <Modal show={show} onHide={onHide} size="xl" centered>
            <Modal.Header closeButton>
                <Modal.Title>Select {category.name}</Modal.Title>
            </Modal.Header>
            <Modal.Body style={{ maxHeight: '70vh', overflowY: 'auto' }}>
                <InputGroup className="mb-3">
                    <Form.Control
                        placeholder={`Search for a ${category.name}...`}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </InputGroup>
                
                {}
                <ExploreComponent 
                    category={category.dbType} 
                    searchQuery={searchQuery} 
                    onProductSelect={handleSelect} 
                />
            </Modal.Body>
        </Modal>
    );
};

export default ComponentSelectorModal;