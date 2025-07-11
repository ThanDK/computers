import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react'; // Added useCallback
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchAllComponents, deleteComponent, updateComponentStock } from '../../services/ComponentService';
import { notifySuccess, notifyError } from '../../services/NotificationService';

import PageHeader from '../../components/PageHeader/PageHeader';
import MainHeader from '../../components/MainHeader/MainHeader';
import { Alert, Spinner, Button, Table, Form, InputGroup, Modal } from 'react-bootstrap';
import { BsSearch, BsPlusCircleFill, BsCheck, BsX, BsArrowCounterclockwise } from 'react-icons/bs';

import './ComponentsPage.css';

import {
    useReactTable, getCoreRowModel, getFilteredRowModel,
    getPaginationRowModel, getSortedRowModel, flexRender
} from '@tanstack/react-table';

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
    
    const [sorting, setSorting] = useState([]);
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    
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

    // MODIFICATION: Extracted data fetching logic into a reusable function.
    // useCallback is used for optimization, so this function isn't recreated on every render.
    const loadData = useCallback(async () => {
        if (!token) return;
        setLoading(true);
        setError(''); // Clear previous errors on refetch
        try {
            // Promise.all ensures we wait for both the API call AND the minimum delay
            const [data] = await Promise.all([
                fetchAllComponents(token),
                new Promise(resolve => setTimeout(resolve, 20)) // Your 0.02s "fun" spinner delay
            ]);
            setComponents(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [token]);


    // MODIFICATION: This useEffect now just calls our reusable function.
    useEffect(() => {
        loadData();
    }, [loadData]); // The dependency is now the stable loadData function.

    useEffect(() => {
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

    const handleImageClick = (imageUrl) => {
        setSelectedImage(imageUrl);
        setShowImageModal(true);
    };

    const handleDelete = async (component) => {
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
    
    // MODIFICATION: The reset handler now also calls loadData to refetch from the server.
    const handleResetFilters = () => {
        setGlobalFilter('');
        setColumnFilters([]);
        loadData(); // This triggers the refetch!
    };

    const columns = useMemo(() => [
        { 
            accessorKey: 'imageUrl', header: 'Image', enableSorting: false, meta: { cellClassName: 'text-center-cell' },
            cell: ({ row }) => {
                const imageUrl = row.original.imageUrl;
                return imageUrl ? 
                    <img src={imageUrl} alt={row.original.name} style={{ width: '50px', height: '50px', objectFit: 'cover', borderRadius: '8px' }} onClick={() => handleImageClick(imageUrl)} className="clickable-image" /> 
                    : 'No Image';
            }
        },
        { 
            accessorKey: 'name', header: 'Name',
            cell: info => <TruncatedText text={info.getValue()} />
        },
        { 
            accessorKey: 'mpn', header: 'MPN',
            cell: info => <TruncatedText text={info.getValue()} />
        },
        { accessorKey: 'type', header: 'Type' },
        { accessorKey: 'price', header: 'Price', cell: info => `฿ ${Number(info.getValue()).toFixed(2)}` },
        { 
            accessorKey: 'quantity', header: 'Stock', meta: { cellClassName: 'text-center-cell' },
            cell: ({ row }) => {
                const isEditing = editingRowId === row.id;
                const component = row.original;
                
                if (isEditing) {
                    return (
                        <div ref={stockEditWrapperRef}>
                            <InputGroup size="sm" style={{width: '120px'}}>
                                <Form.Control 
                                    ref={stockValueInputRef}
                                    type="number" 
                                    defaultValue={component.quantity} 
                                    autoFocus 
                                    onKeyDown={e => {
                                        if (e.key === 'Enter') handleStockUpdate(row, e.target.value);
                                    }} 
                                />
                                <Button variant="outline-success" onClick={() => handleStockUpdate(row, stockValueInputRef.current.value)}><BsCheck /></Button>
                                <Button variant="outline-light" onClick={() => setEditingRowId(null)}><BsX /></Button>
                            </InputGroup>
                        </div>
                    );
                }

                return (
                    <div className="status-dot-container" title="Double-click to edit stock" onDoubleClick={(e) => { 
                        e.stopPropagation(); 
                        setEditingRowId(row.id); 
                    }}>
                        <div className={`status-dot ${component.quantity > 0 ? 'status-dot-active' : 'status-dot-inactive'}`}></div>
                        <span>{component.quantity}</span>
                    </div>
                );
            }
        },
        { 
            id: 'actions', header: 'Actions', meta: { cellClassName: 'text-center-cell' },
            cell: ({ row }) => (
                <div className="d-flex gap-2 justify-content-center">
                    <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => navigate(`/edit-component/${row.original.id}`)}>Edit</Button>
                    <Button variant="outline-danger" size="sm" className="action-btn action-btn-delete" onClick={() => handleDelete(row.original)}>Delete</Button>
                </div>
            )
        }
    ], [editingRowId, token, navigate]);

    const table = useReactTable({
        data: components,
        columns,
        state: { sorting, globalFilter, columnFilters },
        onSortingChange: setSorting,
        onGlobalFilterChange: setGlobalFilter,
        onColumnFiltersChange: setColumnFilters,
        getCoreRowModel: getCoreRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
    });

    const typeFilterValue = table.getColumn('type')?.getFilterValue() || '';

    // MODIFICATION: Check loading state first to show the spinner during refetch.
    if (loading) return (
        <>
            <MainHeader />
            <PageHeader title="Manage Components" subtitle="View, search, and manage product components" />
            <div className="text-center p-5"><Spinner animation="border" /></div>
        </>
    );

    if (error) return (
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
                            table.getColumn('type')?.setFilterValue(value || undefined);
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

            <div className="table-container">
                <Table striped hover responsive variant="dark" className="custom-table">
                    <thead>
                        {table.getHeaderGroups().map(headerGroup => (
                            <tr key={headerGroup.id}>
                                {headerGroup.headers.map(header => (
                                    <th key={header.id} style={{
                                        width: 
                                            header.id === 'imageUrl' ? '10%' :
                                            header.id === 'name' ? '25%' :
                                            header.id === 'mpn' ? '15%' :
                                            header.id === 'type' ? '15%' :
                                            header.id === 'price' ? '10%' :
                                            header.id === 'quantity' ? '10%' :
                                            'auto'
                                    }} className={header.column.columnDef.meta?.cellClassName}
                                    onClick={header.column.getToggleSortingHandler()}>
                                        {flexRender(header.column.columnDef.header, header.getContext())}
                                        {{ asc: ' ▲', desc: ' ▼' }[header.column.getIsSorted()] ?? ''}
                                    </th>
                                ))}
                            </tr>
                        ))}
                    </thead>
                    <tbody>
                        {table.getRowModel().rows.map(row => (
                            <tr key={row.id}>
                                {row.getVisibleCells().map(cell => (
                                    <td key={cell.id} className={cell.column.columnDef.meta?.cellClassName}>
                                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </Table>
            </div>

            <div className="d-flex justify-content-end align-items-center mt-3 gap-2">
                <Button className="pagination-btn" variant="outline-light" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>Previous</Button>
                <span className="mx-2">Page{' '}
                    <strong>
                        {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
                    </strong>
                </span>
                <Button className="pagination-btn" variant="outline-light" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>Next</Button>
            </div>

            <Modal show={showImageModal} onHide={() => setShowImageModal(false)} centered size="lg" className="image-modal">
                <Modal.Header closeButton></Modal.Header>
                <Modal.Body><img src={selectedImage} alt="Fullscreen component view" className="fullscreen-image" /></Modal.Body>
            </Modal>
        </>
    );
}

export default ComponentsPage;