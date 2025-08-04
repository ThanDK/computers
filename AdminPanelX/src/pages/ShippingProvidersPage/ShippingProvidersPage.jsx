// src/pages/ShippingProvidersPage/ShippingProvidersPage.js

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  fetchAllShippingProviders,
  createShippingProvider,
  updateShippingProvider,
  deleteShippingProvider,
} from '../../services/LookupService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';
import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';
import ImageModal from '../../components/ImageModal/ImageModal';
import TableControls from '../../components/TableControls/TableControls';
import ShippingProviderFormModal from '../../components/ShippingProviderFormModal/ShippingProviderFormModal';
import { Button, Image, InputGroup, Form } from 'react-bootstrap';
import { BsPlusCircleFill, BsSearch, BsArrowCounterclockwise } from 'react-icons/bs';
import './ShippingProvidersPage.css';
import '../../components/ImageModal/ImageModal.css';

function ShippingProvidersPage() {
  const { token } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const tableState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams ? JSON.parse(sortingParams) : [];
    const globalFilter = searchParams.get('globalFilter') || '';
    return { pagination: { pageIndex, pageSize }, sorting, globalFilter, columnFilters: [] };
  }, [searchParams]);

  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [imageModalUrl, setImageModalUrl] = useState(null);
  const [pagination, setPagination] = useState(tableState.pagination);
  const [sorting, setSorting] = useState(tableState.sorting);
  const [globalFilter, setGlobalFilter] = useState(tableState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(tableState.columnFilters);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [data] = await Promise.all([
        fetchAllShippingProviders(token),
        new Promise((resolve) => setTimeout(resolve, 20)),
      ]);
      setProviders(data);
    } catch (err) {
      setError(err.message || 'Failed to load shipping providers.');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    const newSearchParams = new URLSearchParams();
    if (pagination.pageIndex > 0) newSearchParams.set('page', pagination.pageIndex.toString());
    if (pagination.pageSize !== 10) newSearchParams.set('pageSize', pagination.pageSize.toString());
    if (sorting.length > 0) newSearchParams.set('sort', JSON.stringify(sorting));
    if (globalFilter) newSearchParams.set('globalFilter', globalFilter);
    setSearchParams(newSearchParams, { replace: true });
  }, [pagination, sorting, globalFilter, setSearchParams]);

  const handleImageClick = useCallback((imageUrl) => { setImageModalUrl(imageUrl); }, []);
  const handleShowModal = (type, item = null) => setModalState({ show: true, type, currentItem: item });
  const handleCloseModal = () => setModalState({ show: false, type: 'add', currentItem: null });

  const handleFormSubmit = async (event, imageFile) => {
    event.preventDefault();
    setIsSubmitting(true);
    const data = Object.fromEntries(new FormData(event.currentTarget).entries());
    const providerData = { name: data.name, trackingUrl: data.trackingUrl };
    const { type, currentItem } = modalState;

    try {
      if (type === 'add') {
        await createShippingProvider(providerData, imageFile, token);
        notifySuccess('Provider created!');
      } else {
        await updateShippingProvider(currentItem.id, providerData, imageFile, token);
        notifySuccess('Provider updated!');
      }
      handleCloseModal();
      loadData();
    } catch (err)      {
      notifyError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = useCallback(async (item) => {
    const confirmed = await showConfirmation('Are you sure?', `This will permanently delete "${item.name}".`);
    if (!confirmed) return;
    try {
      await deleteShippingProvider(item.id, token);
      notifySuccess('Provider deleted!');
      loadData();
    } catch (err) {
      notifyError(err.message);
    }
  }, [token, loadData]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([]);
    loadData();
  };

  const columns = useMemo(
    () => [
      {
        accessorKey: 'imageUrl', header: 'Logo', enableSorting: false, meta: { width: '15%' },
        cell: ({ row }) => {
          const imageUrl = row.original.imageUrl;
          return imageUrl ? (
            <Image src={imageUrl} alt={row.original.name} style={{ width: '50px', height: '50px', objectFit: 'cover', borderRadius: '8px' }} className="clickable-image" onClick={() => handleImageClick(imageUrl)} />
          ) : 'N/A';
        },
      },
      { accessorKey: 'name', header: 'Name' },
      { accessorKey: 'trackingUrl', header: 'Tracking URL', enableSorting: false },
      {
        id: 'actions', header: 'Actions', enableSorting: false, meta: { cellClassName: 'text-center-cell', width: '150px' },
        cell: ({ row }) => (
          <div className="d-flex gap-2 justify-content-center">
            <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => handleShowModal('edit', row.original)}>Edit</Button>
            <Button variant="outline-danger" size="sm" className="action-btn action-btn-delete" onClick={() => handleDelete(row.original)}>Delete</Button>
          </div>
        ),
      },
    ],
    [handleDelete, handleImageClick]
  );

  return (
    <>
      <MainHeader />
      <PageHeader title="Shipping Providers" subtitle="Manage shipping carriers for order fulfillment" />

      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control placeholder="Search providers..." value={globalFilter ?? ''} onChange={(e) => setGlobalFilter(e.target.value)} className="search-input" />
            <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
          </InputGroup>
          <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
            <BsArrowCounterclockwise /> Reset
          </Button>
        </div>
        <Button variant="primary" onClick={() => handleShowModal('add')} className="d-flex align-items-center gap-2">
          <BsPlusCircleFill /> Add New Provider
        </Button>
      </TableControls>

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
        pagination={pagination}
        onPaginationChange={setPagination}
        keepPageOnDataUpdate={true}
      />

      <ShippingProviderFormModal
        show={modalState.show}
        onHide={handleCloseModal}
        provider={modalState.currentItem}
        onSubmit={handleFormSubmit}
        isSubmitting={isSubmitting}
      />
      
      <ImageModal show={!!imageModalUrl} onHide={() => setImageModalUrl(null)} imageUrl={imageModalUrl} altText="Fullscreen provider logo" />
    </>
  );
}

export default ShippingProvidersPage;