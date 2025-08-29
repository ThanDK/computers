import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

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
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  // อ่าน state ของตารางจาก URL
  const tableState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams ? JSON.parse(sortingParams) : [];
    const globalFilter = searchParams.get('globalFilter') || '';

    return { pagination: { pageIndex, pageSize }, sorting, globalFilter, columnFilters: [] };
  }, [searchParams]);

  // ดึงข้อมูลบริษัทขนส่ง
  const { data: providers = [], isLoading, isFetching, error, refetch } = useQuery({
    queryKey: ['shippingProviders'],
    queryFn: async () => {
        const [data] = await Promise.all([
            fetchAllShippingProviders(token),
            new Promise((resolve) => setTimeout(resolve, 20)),
        ]);
        return data;
    },
    enabled: !!token,
  });

  // helper hook สำหรับสร้าง CUD mutations เพื่อลดโค้ดซ้ำ
  const useProviderMutation = (mutationFn, action) => {
    return useMutation({
        mutationFn,
        onSuccess: () => {
            notifySuccess(`Provider ${action} successfully!`);
            queryClient.invalidateQueries({ queryKey: ['shippingProviders'] });
            handleCloseModal();
        },
        onError: (err) => notifyError(err.message),
    });
  };

  const createProviderMutation = useProviderMutation(
    (vars) => createShippingProvider(vars.data, vars.imageFile, token),
    'created'
  );

  const updateProviderMutation = useProviderMutation(
    (vars) => updateShippingProvider(vars.id, vars.data, vars.imageFile, token),
    'updated'
  );

  const deleteProviderMutation = useProviderMutation(
    (item) => deleteShippingProvider(item.id, token),
    'deleted'
  );

  const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });
  const [imageModalUrl, setImageModalUrl] = useState(null);
  
  const [pagination, setPagination] = useState(tableState.pagination);
  const [sorting, setSorting] = useState(tableState.sorting);
  const [globalFilter, setGlobalFilter] = useState(tableState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(tableState.columnFilters);

  // อัปเดต URL search params เมื่อ state ของตารางเปลี่ยน
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

  // จัดการ submit form ทั้ง add และ edit
  const handleFormSubmit = async (formData, imageFile) => {
    const data = Object.fromEntries(formData.entries());
    const { type, currentItem } = modalState;

    if (type === 'add') {
      createProviderMutation.mutate({ data, imageFile });
    } else {
      updateProviderMutation.mutate({ id: currentItem.id, data, imageFile });
    }
  };

  // useCallback ที่นี่สำคัญ เพราะฟังก์ชัน handleDelete ถูกส่งไปเป็น dependency ของ useMemo
  // การใส่ dependency array `[deleteProviderMutation]` ทำให้มั่นใจว่าฟังก์ชันจะถูกสร้างใหม่
  // ก็ต่อเมื่อ `deleteProviderMutation` เปลี่ยนเท่านั้น
  const handleDelete = useCallback(async (item) => {
    const confirmed = await showConfirmation('Are you sure?', `This will permanently delete "${item.name}".`);
    if (confirmed) {
      deleteProviderMutation.mutate(item);
    }
  }, [deleteProviderMutation]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([]);
    refetch();
  };

  // config คอลัมน์สำหรับตาราง
  const columns = useMemo(() => [
    {
      accessorKey: 'imageUrl',
      header: 'Logo',
      enableSorting: false,
      meta: { width: '15%' },
      cell: ({ row }) => {
        const imageUrl = row.original.imageUrl;
        return imageUrl ? (
          <Image
            src={imageUrl}
            alt={row.original.name}
            style={{ width: '50px', height: '50px', objectFit: 'cover', borderRadius: '8px' }}
            className="clickable-image"
            onClick={() => handleImageClick(imageUrl)}
          />
        ) : 'N/A';
      },
    },
    {
      accessorKey: 'name',
      header: 'Name'
    },
    {
      accessorKey: 'trackingUrl',
      header: 'Tracking URL',
      enableSorting: false
    },
    {
      id: 'actions',
      header: 'Actions',
      enableSorting: false,
      meta: { cellClassName: 'text-center-cell', width: '150px' },
      cell: ({ row }) => (
        <div className="d-flex gap-2 justify-content-center">
          <Button
            variant="outline-primary"
            size="sm"
            className="action-btn action-btn-edit"
            onClick={() => handleShowModal('edit', row.original)}
          >
            Edit
          </Button>
          <Button
            variant="outline-danger"
            size="sm"
            className="action-btn action-btn-delete"
            onClick={() => handleDelete(row.original)}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ], [handleDelete, handleImageClick]);
  
  const isSubmitting = createProviderMutation.isPending || updateProviderMutation.isPending;

  return (
    <>
      <MainHeader />
      <PageHeader
        title="Shipping Providers"
        subtitle="Manage shipping carriers for order fulfillment"
      />

      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control
              placeholder="Search providers..."
              value={globalFilter ?? ''}
              onChange={(e) => setGlobalFilter(e.target.value)}
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
      </TableControls>

      <ReusableTable
        columns={columns}
        data={providers}
        isLoading={isLoading || isFetching}
        error={error ? error.message : null}
        pagination={pagination}
        onPaginationChange={setPagination}
        sorting={sorting}
        setSorting={setSorting}
        globalFilter={globalFilter}
        setGlobalFilter={setGlobalFilter}
        columnFilters={columnFilters}
        setColumnFilters={setColumnFilters}
        keepPageOnDataUpdate={true}
      />

      <ShippingProviderFormModal
        show={modalState.show}
        onHide={handleCloseModal}
        provider={modalState.currentItem}
        onSubmit={handleFormSubmit}
        isSubmitting={isSubmitting}
      />
      
      <ImageModal
        show={!!imageModalUrl}
        onHide={() => setImageModalUrl(null)}
        imageUrl={imageModalUrl}
        altText="Fullscreen provider logo"
      />
    </>
  );
}

export default ShippingProvidersPage;