import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { fetchLookupsByType, createLookup, updateLookup, deleteLookup } from '../../services/LookupService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';

import PageHeader from '../../components/PageHeader/PageHeader';
import MainHeader from '../../components/MainHeader/MainHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';

import { Tabs, Tab, Button, Modal, Form, Alert, Row, Col, InputGroup } from 'react-bootstrap';
import { BsPlusCircleFill, BsSearch, BsArrowCounterclockwise } from 'react-icons/bs';
import './LookupsPage.css';

const lookupConfig = {
    sockets: { title: 'Sockets', columns: ['name', 'brand'], fields: ['name', 'brand'] },
    'ram-types': { title: 'RAM Types', columns: ['name'], fields: ['name'] },
    'form-factors': { title: 'Form Factors', columns: ['name', 'type'], fields: ['name', 'type'] },
    'storage-interfaces': { title: 'Storage Interfaces', columns: ['name'], fields: ['name'] }
};

const formFactorTypes = ['MOTHERBOARD', 'PSU', 'STORAGE'];

function LookupsPage() {
    const { token } = useAuth();
    const [lookups, setLookups] = useState({ sockets: [], 'ram-types': [], 'form-factors': [], 'storage-interfaces': [] });
    const [activeTab, setActiveTab] = useState('sockets');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });

    const [sorting, setSorting] = useState([]);
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);

    const loadLookupsForTab = useCallback(async (tabKey) => {
        if (!token) return;
        setLoading(true);
        setError('');
        try {
            // --- MODIFICATION: Matched delay to ComponentsPage ---
            const [data] = await Promise.all([
                fetchLookupsByType(tabKey, token),
                new Promise(resolve => setTimeout(resolve, 20)) // Use 20ms delay
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
        loadLookupsForTab(activeTab);
    }, [activeTab]);

    const handleTabSelect = (tabKey) => {
        setActiveTab(tabKey);
        setGlobalFilter('');
        setColumnFilters([]);
        setSorting([]);
    };

    const handleShowModal = (type, item = null) => setModalState({ show: true, type, currentItem: item });
    const handleCloseModal = () => setModalState({ show: false, type: 'add', currentItem: null });

    const handleFormSubmit = async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());
        const { type, currentItem } = modalState;

        try {
            if (type === 'add') {
                await createLookup(activeTab, data, token);
                notifySuccess(`${lookupConfig[activeTab].title.slice(0, -1)} created!`);
            } else {
                await updateLookup(activeTab, currentItem.id, data, token);
                notifySuccess(`${lookupConfig[activeTab].title.slice(0, -1)} updated!`);
            }
            handleCloseModal();
            loadLookupsForTab(activeTab);
        } catch (err) {
            notifyError(err.message);
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
        const baseColumns = lookupConfig[activeTab].columns.map(key => ({
            accessorKey: key,
            header: key.charAt(0).toUpperCase() + key.slice(1),
        }));
        
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
        const fields = lookupConfig[activeTab].fields;
        const { currentItem } = modalState;
        
        return (
            <Row>
                {fields.map((field, index) => (
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
                        <Button variant="primary" type="submit">Save Changes</Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </>
    );
}

export default LookupsPage;