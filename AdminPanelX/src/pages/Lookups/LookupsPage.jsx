// src/pages/LookupsPage/LookupsPage.js
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
    fetchLookupsByType, createLookup, updateLookup, deleteLookup,
    createBrand, updateBrand
} from '../../services/LookupService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';

import PageHeader from '../../components/PageHeader/PageHeader';
import MainHeader from '../../components/MainHeader/MainHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';

// MODIFIED: Imported Spinner
import { Tabs, Tab, Button, Modal, Form, Alert, Row, Col, InputGroup, Image, Spinner } from 'react-bootstrap';
import { BsPlusCircleFill, BsSearch, BsArrowCounterclockwise } from 'react-icons/bs';
import './LookupsPage.css';

const lookupConfig = {
    sockets: { title: 'Sockets', columns: ['name', 'brand'], fields: ['name', 'brand'] },
    'ram-types': { title: 'RAM Types', columns: ['name'], fields: ['name'] },
    'form-factors': { title: 'Form Factors', columns: ['name', 'type'], fields: ['name', 'type'] },
    'storage-interfaces': { title: 'Storage Interfaces', columns: ['name'], fields: ['name'] },
    brands: { title: 'Brands', columns: ['logoUrl', 'name'], fields: ['name'], hasImage: true },
};

const formFactorTypes = ['MOTHERBOARD', 'PSU', 'STORAGE'];

function LookupsPage() {
    const { token } = useAuth();
    const [lookups, setLookups] = useState({ sockets: [], 'ram-types': [], 'form-factors': [], 'storage-interfaces': [], brands: [] });
    const [activeTab, setActiveTab] = useState('sockets');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });

    const [imageFile, setImageFile] = useState(null);
    const [imagePreviewUrl, setImagePreviewUrl] = useState(null);
    
    const [isSubmitting, setIsSubmitting] = useState(false);

    const [sorting, setSorting] = useState([]);
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);

    const loadLookupsForTab = useCallback(async (tabKey) => {
        if (!token) return;
        setLoading(true);
        setError('');
        try {
            const [data] = await Promise.all([
                fetchLookupsByType(tabKey, token),
                new Promise(resolve => setTimeout(resolve, 20))
            ]);
            setLookups(prev => ({ ...prev, [tabKey]: data }));
        } catch (err) {
            setError(err.message || `Failed to load ${tabKey}.`);
            setLookups(prev => ({ ...prev, [tabKey]: [] }));
        } finally {
            setLoading(false);
        }
    }, [token]);
    
    useEffect(() => {
        if (modalState.show && lookupConfig[activeTab]?.hasImage) {
            setImagePreviewUrl(modalState.currentItem?.logoUrl || null);
        }
    }, [modalState.show, modalState.currentItem, activeTab]);

    useEffect(() => {
        loadLookupsForTab(activeTab);
    }, [activeTab]);

    const handleTabSelect = (tabKey) => {
        setActiveTab(tabKey);
        setGlobalFilter('');
        setColumnFilters([]);
        setSorting([]);
    };

    const handleShowModal = (type, item = null) => setModalState({ show: true, type, currentItem: item });

    const handleCloseModal = () => {
        setModalState({ show: false, type: 'add', currentItem: null });
        setImageFile(null);
        setImagePreviewUrl(null);
    };

    const handleImageChange = (event) => {
        const file = event.target.files[0];
        if (file) {
            setImageFile(file);
            setImagePreviewUrl(URL.createObjectURL(file));
        }
    };

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        
        // --- FIX: Set submitting to true to disable button ---
        setIsSubmitting(true);

        const form = event.currentTarget;
        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());
        
        const { type, currentItem } = modalState;
        const config = lookupConfig[activeTab];

        try {
            if (config.hasImage) {
                const brandData = { name: data.name };
                if (type === 'add') {
                    await createBrand(brandData, imageFile, token);
                    notifySuccess(`${config.title.slice(0, -1)} created!`);
                } else {
                    await updateBrand(currentItem.id, brandData, imageFile, token);
                    notifySuccess(`${config.title.slice(0, -1)} updated!`);
                }
            } else {
                if (type === 'add') {
                    await createLookup(activeTab, data, token);
                    notifySuccess(`${config.title.slice(0, -1)} created!`);
                } else {
                    await updateLookup(activeTab, currentItem.id, data, token);
                    notifySuccess(`${config.title.slice(0, -1)} updated!`);
                }
            }
            handleCloseModal();
            loadLookupsForTab(activeTab);
        } catch (err) {
            notifyError(err.message);
        } finally {
            // --- FIX: Set submitting back to false after operation completes ---
            setIsSubmitting(false);
        }
    };

    const handleDelete = async (item) => {
        const isConfirmed = await showConfirmation('Are you sure?', `This will permanently delete "${item.name}".`);
        if (!isConfirmed) return;

        try {
            await deleteLookup(activeTab, item.id, token);
            notifySuccess(`${lookupConfig[activeTab].title.slice(0, -1)} deleted!`);
            loadLookupsForTab(activeTab);
        } catch (err) {
            notifyError(err.message);
        }
    };

    const handleResetFilters = () => {
        setGlobalFilter('');
        setColumnFilters([]);
        loadLookupsForTab(activeTab);
    };

    const columns = useMemo(() => {
        const config = lookupConfig[activeTab];
        const baseColumns = config.columns.map(key => {
            if (key === 'logoUrl') {
                return {
                    accessorKey: 'logoUrl',
                    header: 'Logo',
                    enableSorting: false,
                    meta: { width: '40%' },
                    cell: info => info.getValue() ? <Image src={info.getValue()} className="table-logo" /> : 'N/A'
                };
            }
            return {
                accessorKey: key,
                header: key.charAt(0).toUpperCase() + key.slice(1),
            };
        });
        
        baseColumns.push({
            id: 'actions',
            header: 'Actions',
            enableSorting: false,
            meta: { cellClassName: 'text-center-cell', width: '150px' },
            cell: ({ row }) => (
                <div className="d-flex gap-2 justify-content-center">
                    <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => handleShowModal('edit', row.original)}>Edit</Button>
                    <Button variant="outline-danger" size="sm" className="action-btn action-btn-delete" onClick={() => handleDelete(row.original)}>Delete</Button>
                </div>
            )
        });
        return baseColumns;
    }, [activeTab, token]);

    const renderModalFormBody = () => {
        const config = lookupConfig[activeTab];
        const { currentItem } = modalState;

        if (config.hasImage) {
            return (
                <Row>
                    <Col>
                        {imagePreviewUrl && (
                            <div className="text-center mb-3">
                                <Image src={imagePreviewUrl} alt="Logo preview" className="logo-preview" />
                            </div>
                        )}
                        <Form.Group className="mb-3">
                            <Form.Label>Brand Name</Form.Label>
                            <Form.Control name="name" type="text" required defaultValue={currentItem?.name || ''} autoFocus />
                        </Form.Group>
                        <Form.Group>
                            <Form.Label>Brand Logo</Form.Label>
                            <Form.Control name="image" type="file" accept="image/*" onChange={handleImageChange} />
                        </Form.Group>
                    </Col>
                </Row>
            );
        }

        return (
            <Row>
                {config.fields.map((field, index) => (
                    <Col md={12} key={field}>
                        <Form.Group className="mb-3">
                            <Form.Label>{field.charAt(0).toUpperCase() + field.slice(1)}</Form.Label>
                            {field === 'type' ? (
                                <Form.Select name="type" required defaultValue={currentItem?.type || ''}>
                                    <option value="" disabled>-- Select Type --</option>
                                    {formFactorTypes.map(type => <option key={type} value={type}>{type}</option>)}
                                </Form.Select>
                            ) : (
                                <Form.Control type="text" name={field} required autoFocus={index === 0} defaultValue={currentItem?.[field] || ''}/>
                            )}
                        </Form.Group>
                    </Col>
                ))}
            </Row>
        );
    };

    const typeFilterValue = columnFilters.find(f => f.id === 'type')?.value || '';

    return (
        <>
            <MainHeader />
            <PageHeader title="Manage Lookups" subtitle="Add, edit, or delete data used in component forms" />

            <Tabs id="lookups-tabs" activeKey={activeTab} onSelect={handleTabSelect} className="mb-3 lookups-tabs">
                {Object.keys(lookupConfig).map(key => (
                    <Tab eventKey={key} title={lookupConfig[key].title} key={key} >
                        <div className="table-controls-container">
                            <div className="filter-controls">
                                <InputGroup className="search-bar">
                                    <Form.Control 
                                        placeholder={`Search ${lookupConfig[key].title}...`}
                                        value={globalFilter ?? ''}
                                        onChange={e => setGlobalFilter(e.target.value)}
                                        className="search-input"
                                    />
                                    <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
                                </InputGroup>

                                {key === 'form-factors' && (
                                    <Form.Select 
                                        className="type-filter"
                                        aria-label="Filter by form factor type"
                                        value={typeFilterValue}
                                        onChange={e => {
                                            const value = e.target.value;
                                            setColumnFilters(prev => 
                                                prev.filter(f => f.id !== 'type').concat(value ? [{ id: 'type', value }] : [])
                                            );
                                        }}
                                    >
                                        <option value="">All Types</option>
                                        {formFactorTypes.map(type => (
                                            <option key={type} value={type}>{type}</option>
                                        ))}
                                    </Form.Select>
                                )}

                                <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
                                    <BsArrowCounterclockwise /> Reset
                                </Button>
                            </div>
                            <Button variant="primary" onClick={() => handleShowModal('add')} className="d-flex align-items-center gap-2">
                                <BsPlusCircleFill /> Add New {lookupConfig[key].title.slice(0, -1)}
                            </Button>
                        </div>
                        
                        <ReusableTable
                            columns={columns}
                            data={lookups[key]}
                            isLoading={loading}
                            error={error && !lookups[key].length ? error : null}
                            sorting={sorting}
                            setSorting={setSorting}
                            globalFilter={globalFilter}
                            setGlobalFilter={setGlobalFilter}
                            columnFilters={columnFilters}
                            setColumnFilters={setColumnFilters}
                        />
                    </Tab>
                ))}
            </Tabs>

            <Modal show={modalState.show} onHide={handleCloseModal} centered className="lookup-modal">
                <Modal.Header closeButton>
                    <Modal.Title>{modalState.type === 'add' ? 'Add New' : 'Edit'} {lookupConfig[activeTab]?.title.slice(0, -1)}</Modal.Title>
                </Modal.Header>
                <Form onSubmit={handleFormSubmit}>
                    <Modal.Body>{renderModalFormBody()}</Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={handleCloseModal}>Cancel</Button>
                        
                        {/* --- FIX: Updated Button with Spinner --- */}
                        <Button variant="primary" type="submit" disabled={isSubmitting}>
                            {isSubmitting ? (
                                <>
                                    <Spinner
                                        as="span"
                                        animation="border"
                                        size="sm"
                                        role="status"
                                        aria-hidden="true"
                                    />
                                    <span className="ms-2">Saving...</span>
                                </>
                            ) : (
                                'Save Changes'
                            )}
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </>
    );
}

export default LookupsPage;