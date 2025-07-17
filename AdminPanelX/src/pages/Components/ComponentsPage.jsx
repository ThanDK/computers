// src/pages/ComponentsPage/ComponentsPage.js

import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchAllComponents, deleteComponent, updateComponentStock } from '../../services/ComponentService';
import { notifySuccess, notifyError } from '../../services/NotificationService';

import PageHeader from '../../components/PageHeader/PageHeader';
import MainHeader from '../../components/MainHeader/MainHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable'; // <-- IMPORT THE NEW COMPONENT
import { Alert, Spinner, Button, Form, InputGroup, Modal } from 'react-bootstrap';
import { BsSearch, BsPlusCircleFill, BsCheck, BsX, BsArrowCounterclockwise } from 'react-icons/bs';

import './ComponentsPage.css';

// TruncatedText component remains the same
const TruncatedText = ({ text }) => {
    const handleDoubleClick = () => {
        navigator.clipboard.writeText(text);
        notifySuccess(`Copied "${text}"`);
    };
    return (
        <div 
            className="truncate-text" 
            onDoubleClick={handleDoubleClick}
            title={`Double-click to copy: ${text}`}
        >
            {text}
        </div>
    );
};

function ComponentsPage() {
    const navigate = useNavigate();
    const { token } = useAuth();
    const [components, setComponents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    // Table state remains here
    const [sorting, setSorting] = useState([]);
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    
    // Component-specific state remains here
    const [editingRowId, setEditingRowId] = useState(null);
    const [showImageModal, setShowImageModal] = useState(false);
    const [selectedImage, setSelectedImage] = useState('');
    const stockEditWrapperRef = useRef(null);
    const stockValueInputRef = useRef(null);

    const uniqueTypes = useMemo(() => {
        if (components.length === 0) return [];
        const types = new Set(components.map(c => c.type).filter(Boolean));
        return Array.from(types).sort();
    }, [components]);

    const loadData = useCallback(async () => {
        if (!token) return;
        setLoading(true);
        setError('');
        try {
            const [data] = await Promise.all([
                fetchAllComponents(token),
                new Promise(resolve => setTimeout(resolve, 20))
            ]);
            setComponents(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    useEffect(() => {
        // ... (useEffect for click away logic is unchanged)
        const handleClickAway = (event) => {
            if (editingRowId && stockEditWrapperRef.current && !stockEditWrapperRef.current.contains(event.target)) {
                setEditingRowId(null);
            }
        };
        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                setEditingRowId(null);
            }
        };
        document.addEventListener('mousedown', handleClickAway);
        document.addEventListener('keydown', handleEscape);
        return () => {
            document.removeEventListener('mousedown', handleClickAway);
            document.removeEventListener('keydown', handleEscape);
        };
    }, [editingRowId]);

    // All handlers remain here as they are specific to "Components"
    const handleImageClick = (imageUrl) => {
        setSelectedImage(imageUrl);
        setShowImageModal(true);
    };

    const handleDelete = async (component) => {
        // ... (unchanged)
        try {
            const wasDeleted = await deleteComponent(component, token);
            if (wasDeleted) {
                setComponents(prev => prev.filter(c => c.id !== component.id));
            }
        } catch (err) {
            console.error("Deletion process failed:", err);
        }
    };
    
    const handleStockUpdate = async (row, newStockValueStr) => {
        // ... (unchanged)
        const newStockValue = Number(newStockValueStr);
        const component = row.original;
        if (isNaN(newStockValue) || newStockValue < 0) {
            notifyError("Invalid stock. Please enter a positive number.");
            return;
        }
        const quantityChange = newStockValue - component.quantity;
        if (quantityChange === 0) {
            setEditingRowId(null);
            return;
        }
        try {
            const updatedComponent = await updateComponentStock(component.id, quantityChange, token);
            setComponents(prev => prev.map(c => c.id === updatedComponent.id ? updatedComponent : c));
        } catch (err) {
            console.error("Stock update process failed:", err);
        } finally {
            setEditingRowId(null);
        }
    };
    
    const handleResetFilters = () => {
        setGlobalFilter('');
        setColumnFilters([]);
        loadData();
    };


    const columns = useMemo(() => [
        { 
            accessorKey: 'imageUrl', header: 'Image', enableSorting: false, 
            meta: { cellClassName: 'text-center-cell', width: '10%' }, // <-- Control width here
            cell: ({ row }) => {
                const imageUrl = row.original.imageUrl;
                return imageUrl ? 
                    <img src={imageUrl} alt={row.original.name} style={{ width: '50px', height: '50px', objectFit: 'cover', borderRadius: '8px' }} onClick={() => handleImageClick(imageUrl)} className="clickable-image" /> 
                    : 'No Image';
            }
        },
        { 
            accessorKey: 'name', header: 'Name',
            meta: { width: '20%' }, // <-- Control width here
            cell: info => <TruncatedText text={info.getValue()} />
        },
        { 
            accessorKey: 'mpn', header: 'MPN',
            meta: { width: '15%' }, // <-- Control width here
            cell: info => <TruncatedText text={info.getValue()} />
        },
        { 
            accessorKey: 'type', header: 'Type',
            meta: { width: '15%' } // <-- Control width here
        },
        { 
            accessorKey: 'price', header: 'Price', 
            meta: { width: '10%' }, // <-- Control width here
            cell: info => `฿ ${Number(info.getValue()).toFixed(2)}` 
        },
        { 
            accessorKey: 'quantity', header: 'Stock', 
            meta: { cellClassName: 'text-center-cell', width: '10%' }, // <-- Control width here
            cell: ({ row }) => {
                 // ... (stock editing cell logic is unchanged)
                const isEditing = editingRowId === row.id;
                const component = row.original;
                if (isEditing) {
                    return (
                        <div ref={stockEditWrapperRef}>
                            <InputGroup size="sm" style={{width: '120px'}}>
                                <Form.Control ref={stockValueInputRef} type="number" defaultValue={component.quantity} autoFocus 
                                    onKeyDown={e => { if (e.key === 'Enter') handleStockUpdate(row, e.target.value); }} />
                                <Button variant="outline-success" onClick={() => handleStockUpdate(row, stockValueInputRef.current.value)}><BsCheck /></Button>
                                <Button variant="outline-light" onClick={() => setEditingRowId(null)}><BsX /></Button>
                            </InputGroup>
                        </div>
                    );
                }
                return (
                    <div className="status-dot-container" title="Double-click to edit stock" onDoubleClick={(e) => { e.stopPropagation(); setEditingRowId(row.id); }}>
                        <div className={`status-dot ${component.quantity > 0 ? 'status-dot-active' : 'status-dot-inactive'}`}></div>
                        <span>{component.quantity}</span>
                    </div>
                );
            }
        },
        { 
            id: 'actions', header: 'Actions', 
            meta: { cellClassName: 'text-center-cell', width: 'auto' }, 
            cell: ({ row }) => (
                <div className="d-flex gap-2 justify-content-center">
                    <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => navigate(`/edit-component/${row.original.id}`)}>Edit</Button>
                    <Button variant="outline-danger" size="sm" className="action-btn action-btn-delete" onClick={() => handleDelete(row.original)}>Delete</Button>
                </div>
            )
        }
    ], [editingRowId, token, navigate]); 


    const typeFilterValue = columnFilters.find(f => f.id === 'type')?.value || '';

    // The main loading/error checks can happen here before rendering the whole page layout
    if (loading && !components.length) return (
        <>
            <MainHeader />
            <PageHeader title="Manage Components" subtitle="View, search, and manage product components" />
            <div className="text-center p-5"><Spinner animation="border" /></div>
        </>
    );

    if (error && !components.length) return (
         <>
            <MainHeader />
            <PageHeader title="Manage Components" subtitle="View, search, and manage product components" />
            <Alert variant="danger" className="m-4">{error}</Alert>
        </>
    );

    return (
        <>
            <MainHeader />
            <PageHeader title="Manage Components" subtitle="View, search, and manage product components" />
            
            {/* Filter controls remain here as they control this page's state */}
            <div className="table-controls-container">
                <div className="filter-controls">
                    <InputGroup className="search-bar">
                        <Form.Control 
                            placeholder="Search all components..."
                            value={globalFilter ?? ''}
                            onChange={e => setGlobalFilter(e.target.value)}
                            className="search-input"
                        />
                        <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
                    </InputGroup>

                    <Form.Select 
                        className="type-filter"
                        aria-label="Filter by component type"
                        value={typeFilterValue}
                        onChange={e => {
                            const value = e.target.value;
                            // Update the columnFilters state
                            setColumnFilters(prev => 
                                prev.filter(f => f.id !== 'type').concat(value ? [{ id: 'type', value }] : [])
                            );
                        }}
                    >
                        <option value="">All Types</option>
                        {uniqueTypes.map(type => (
                            <option key={type} value={type}>{type}</option>
                        ))}
                    </Form.Select>
                    
                    <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
                        <BsArrowCounterclockwise /> Reset
                    </Button>
                </div>

                <Button variant="primary" size="lg" className="d-flex align-items-center gap-2" onClick={() => navigate('/add-component')}>
                    <BsPlusCircleFill /> Add New Component
                </Button>
            </div>

            <ReusableTable
                columns={columns}
                data={components}
                isLoading={loading}
                error={error}
                sorting={sorting}
                setSorting={setSorting}
                globalFilter={globalFilter}
                setGlobalFilter={setGlobalFilter}
                columnFilters={columnFilters}
                setColumnFilters={setColumnFilters}
            />

            <Modal show={showImageModal} onHide={() => setShowImageModal(false)} centered size="lg" className="image-modal">
                <Modal.Header closeButton></Modal.Header>
                <Modal.Body><img src={selectedImage} alt="Fullscreen component view" className="fullscreen-image" /></Modal.Body>
            </Modal>
        </>
    );
}

export default ComponentsPage;