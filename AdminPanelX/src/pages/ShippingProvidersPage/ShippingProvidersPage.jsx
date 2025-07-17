// src/pages/ShippingProvidersPage/ShippingProvidersPage.js
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
    fetchAllShippingProviders, createShippingProvider,
    updateShippingProvider, deleteShippingProvider
} from '../../services/LookupService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';

import { Button, Image, Modal, Form, InputGroup, Row, Col } from 'react-bootstrap';
import { BsPlusCircleFill, BsSearch, BsArrowCounterclockwise } from 'react-icons/bs';

import './ShippingProvidersPage.css';

// --- CHANGE: The modal has been enhanced to handle image previews ---
function ProviderFormModal({ show, onHide, provider, onSubmit }) {
    // State to hold the URL for the image preview
    const [imagePreviewUrl, setImagePreviewUrl] = useState(null);

    // Effect to set the initial preview when the modal opens or the provider changes
    useEffect(() => {
        // If editing, show the existing provider's image. Otherwise, show nothing.
        setImagePreviewUrl(provider?.imageUrl || null);
    }, [provider, show]);

    // Cleanup effect to prevent memory leaks from object URLs
    useEffect(() => {
        return () => {
            if (imagePreviewUrl && imagePreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(imagePreviewUrl);
            }
        };
    }, [imagePreviewUrl]);

    const handleImageChange = (event) => {
        const file = event.target.files[0];
        if (file) {
            // Create a temporary URL for the selected file to show a preview
            setImagePreviewUrl(URL.createObjectURL(file));
        }
    };

    return (
        <Modal show={show} onHide={onHide} centered className="provider-form-modal">
            <Modal.Header closeButton>
                <Modal.Title>{provider ? 'Edit' : 'Add New'} Shipping Provider</Modal.Title>
            </Modal.Header>
            <Form onSubmit={onSubmit}>
                <Modal.Body>
                    <Row>
                        <Col>
                            {/* --- CHANGE: Image preview is displayed here --- */}
                            {imagePreviewUrl && (
                                <div className="text-center mb-3">
                                    <Image src={imagePreviewUrl} alt="Provider logo preview" className="logo-preview" />
                                </div>
                            )}
                            <Form.Group className="mb-3">
                                <Form.Label>Provider Name</Form.Label>
                                <Form.Control name="name" type="text" required defaultValue={provider?.name || ''} autoFocus />
                            </Form.Group>
                            <Form.Group className="mb-3">
                                <Form.Label>Tracking URL (use "{`{trackingNumber}`}" as placeholder)</Form.Label>
                                <Form.Control name="trackingUrl" type="text" placeholder="e.g., https://..." defaultValue={provider?.trackingUrl || ''} />
                            </Form.Group>
                            <Form.Group>
                                <Form.Label>Provider Logo</Form.Label>
                                {/* --- CHANGE: onChange handler added for preview --- */}
                                <Form.Control name="image" type="file" accept="image/*" onChange={handleImageChange} />
                            </Form.Group>
                        </Col>
                    </Row>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={onHide}>Cancel</Button>
                    <Button variant="primary" type="submit">Save Changes</Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
}

function ShippingProvidersPage() {
    const { token } = useAuth();
    const [providers, setProviders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });

    const [sorting, setSorting] = useState([]);
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);

    const loadData = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            // --- CHANGE: Added a small delay for a smoother perceived loading experience, like in LookupsPage ---
            const [data] = await Promise.all([
                fetchAllShippingProviders(token),
                new Promise(resolve => setTimeout(resolve, 20))
            ]);
            setProviders(data);
        } catch (err) {
            setError(err.message || "Failed to load shipping providers.");
            notifyError(err.message || "Failed to load shipping providers.");
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => {
        loadData();
    }, [loadData]);
    
    const handleShowModal = (type, item = null) => setModalState({ show: true, type, currentItem: item });
    const handleCloseModal = () => setModalState({ show: false, type: 'add', currentItem: null });

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        const formData = new FormData(form);
        const name = formData.get('name');
        const trackingUrl = formData.get('trackingUrl');
        const imageFile = formData.get('image');

        const { type, currentItem } = modalState;
        const providerData = { name, trackingUrl };

        try {
            if (type === 'add') {
                await createShippingProvider(providerData, imageFile, token);
                notifySuccess('Provider created!');
            } else {
                await updateShippingProvider(currentItem.id, providerData, imageFile, token);
                notifySuccess('Provider updated!');
            }
            handleCloseModal();
            loadData(); // Refresh data
        } catch (err) {
            notifyError(err.message);
        }
    };
    
    const handleDelete = async (item) => {
        const isConfirmed = await showConfirmation('Are you sure?', `This will permanently delete "${item.name}".`);
        if (!isConfirmed) return;

        try {
            await deleteShippingProvider(item.id, token);
            notifySuccess('Provider deleted!');
            loadData(); // Refresh data
        } catch (err) {
            notifyError(err.message);
        }
    };

    // --- CHANGE: The reset button now also re-fetches the data, just like in LookupsPage ---
    const handleResetFilters = () => {
        setGlobalFilter('');
        setColumnFilters([]);
        loadData(); // Re-fetch the data to ensure the list is fresh
    };

    const columns = useMemo(() => [
        {
            accessorKey: 'imageUrl', header: 'Logo', enableSorting: false,
            meta: { width: '10%' },
            cell: info => info.getValue() ? <Image src={info.getValue()} className="table-logo" /> : 'N/A'
        },
        { 
            accessorKey: 'name', header: 'Name' 
        },
        { 
            accessorKey: 'trackingUrl', header: 'Tracking URL', enableSorting: false 
        },
        {
            id: 'actions', header: 'Actions', enableSorting: false,
            meta: { cellClassName: 'text-center-cell', width: '150px' },
            cell: ({ row }) => (
                <div className="d-flex gap-2 justify-content-center">
                    <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => handleShowModal('edit', row.original)}>Edit</Button>
                    <Button variant="outline-danger" size="sm" className="action-btn action-btn-delete" onClick={() => handleDelete(row.original)}>Delete</Button>
                </div>
            )
        }
    ], [loadData]);

    return (
        <>
            <MainHeader />
            <PageHeader
                title="Shipping Providers"
                subtitle="Manage shipping carriers for order fulfillment"
            />
            
            <div className="table-controls-container">
                <div className="filter-controls">
                    <InputGroup className="search-bar">
                        <Form.Control 
                            placeholder="Search providers..."
                            value={globalFilter ?? ''}
                            onChange={e => setGlobalFilter(e.target.value)}
                            className="search-input"
                        />
                        <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
                    </InputGroup>
                    
                    <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
                        <BsArrowCounterclockwise /> Reset
                    </Button>
                </div>

                <Button variant="primary" onClick={() => handleShowModal('add')} className="d-flex align-items-center gap-2">
                    <BsPlusCircleFill /> Add New Provider
                </Button>
            </div>
            
            <ReusableTable
                columns={columns}
                data={providers}
                isLoading={loading}
                error={error && !providers.length ? error : null}
                sorting={sorting}
                setSorting={setSorting}
                globalFilter={globalFilter}
                setGlobalFilter={setGlobalFilter}
                columnFilters={columnFilters}
                setColumnFilters={setColumnFilters}
            />

            <ProviderFormModal 
                show={modalState.show}
                onHide={handleCloseModal}
                provider={modalState.currentItem}
                onSubmit={handleFormSubmit}
            />
        </>
    );
}

export default ShippingProvidersPage;