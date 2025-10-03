import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import {
  fetchAllPaymentMethods,
  createPaymentMethod,
  updatePaymentMethod,
  deletePaymentMethod,
  setDefaultPaymentMethod,
} from '../../services/PaymentService';
import { notifySuccess, notifyError, showConfirmation, handlePromise } from '../../services/NotificationService';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';
import ImageModal from '../../components/ImageModal/ImageModal';
import TableControls from '../../components/TableControls/TableControls';
import PaymentMethodFormModal from '../../components/PaymentMethodFormModal/PaymentMethodFormModal';
import StatusBadge from '../../components/StatusBadge/StatusBadge';

import { Button, Image, InputGroup, Form } from 'react-bootstrap';
import { BsPlusCircleFill, BsSearch, BsArrowCounterclockwise, BsCheckCircleFill } from 'react-icons/bs';

import './PaymentManagementPage.css';

function PaymentManagementPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  const tableState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams ? JSON.parse(sortingParams) : [];
    const globalFilter = searchParams.get('globalFilter') || '';
    return { pagination: { pageIndex, pageSize }, sorting, globalFilter, columnFilters: [] };
  }, [searchParams]);

  const { data: methods = [], isLoading, isFetching, error, refetch } = useQuery({
    queryKey: ['paymentMethods'],
    queryFn: () => fetchAllPaymentMethods(token),
    enabled: !!token,
  });

  const usePaymentMethodMutation = (mutationFn, action, invalidate = true) => {
    return useMutation({
        mutationFn,
        onSuccess: () => {
            if (invalidate) {
                queryClient.invalidateQueries({ queryKey: ['paymentMethods'] });
            }
            notifySuccess(`Payment method ${action} successfully!`);
            handleCloseModal();
        },
        onError: (err) => notifyError(err.message),
    });
  };

  const createMethodMutation = usePaymentMethodMutation(
    (vars) => createPaymentMethod(vars.data, vars.imageFile, token),
    'created'
  );

  const updateMethodMutation = usePaymentMethodMutation(
    (vars) => updatePaymentMethod(vars.id, vars.data, vars.imageFile, token),
    'updated'
  );

  const deleteMethodMutation = usePaymentMethodMutation(
    (item) => deletePaymentMethod(item.id, token),
    'deleted'
  );

  const setDefaultMutation = useMutation({
    mutationFn: (item) => setDefaultPaymentMethod(item.id, token),
    onSuccess: (updatedDefaultMethod) => {
        queryClient.setQueryData(['paymentMethods'], (oldData) => {
            return oldData.map(method => {
                if (method.id === updatedDefaultMethod.id) {
                    return { ...method, isDefault: true };
                }
                if (method.isDefault) {
                    return { ...method, isDefault: false };
                }
                return method;
            });
        });
        return updatedDefaultMethod;
    }
  });

  const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });
  const [imageModalUrl, setImageModalUrl] = useState(null);
  
  const [pagination, setPagination] = useState(tableState.pagination);
  const [sorting, setSorting] = useState(tableState.sorting);
  const [globalFilter, setGlobalFilter] = useState(tableState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(tableState.columnFilters);

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

  const handleFormSubmit = async (requestData, imageFile) => {
    const { type, currentItem } = modalState;
    if (type === 'add') {
      createMethodMutation.mutate({ data: requestData, imageFile });
    } else {
      updateMethodMutation.mutate({ id: currentItem.id, data: requestData, imageFile });
    }
  };

  const handleDelete = useCallback(async (item) => {
    const confirmed = await showConfirmation('Are you sure?', `This will permanently delete the method for "${item.bankName}".`);
    if (confirmed) {
      deleteMethodMutation.mutate(item);
    }
  }, [deleteMethodMutation]);

  const handleSetDefault = useCallback(async (item) => {
    const promise = setDefaultMutation.mutateAsync(item);
    handlePromise(promise, {
        loading: 'Setting as default...',
        success: (updatedMethod) => `"${updatedMethod.bankName}" is now the default.`,
        error: (err) => `Failed to set default. ${err.message}`
    });
  }, [setDefaultMutation]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([]);
    refetch();
  };

  const columns = useMemo(() => [
    {
      accessorKey: 'qrCodeImageUrl',
      header: 'QR Code',
      enableSorting: false,
      meta: { width: '15%' },
      cell: ({ row }) => {
        const imageUrl = row.original.qrCodeImageUrl;
        return imageUrl ? (
          <Image
            src={imageUrl}
            alt={row.original.bankName}
            style={{ width: '60px', height: '60px', objectFit: 'contain' }}
            className="clickable-image"
            onClick={() => handleImageClick(imageUrl)}
          />
        ) : 'N/A';
      },
    },
    {
      accessorKey: 'bankName',
      header: 'Bank Name'
    },
    {
      accessorKey: 'accountName',
      header: 'Account Name'
    },
    {
      accessorKey: 'accountNumber',
      header: 'Account Number'
    },
    {
      accessorKey: 'isDefault',
      header: 'Status',
      meta: { cellClassName: 'text-center-cell', width: '120px' },
      cell: ({ row }) => row.original.isDefault ? <StatusBadge type="generic" status={row.original.isDefault} /> : null,
    },
    {
      id: 'actions',
      header: 'Actions',
      enableSorting: false,
      meta: { cellClassName: 'text-center-cell', width: '250px' },
      cell: ({ row }) => (
        <div className="d-flex gap-2 justify-content-center">
          {!row.original.isDefault && (
            <Button
              variant="outline-success"
              size="sm"
              className="action-btn"
              onClick={() => handleSetDefault(row.original)}
              title="Set as Default"
            >
              <BsCheckCircleFill />
            </Button>
          )}
          <Button
            variant="outline-primary"
            size="sm"
            className="action-btn"
            onClick={() => handleShowModal('edit', row.original)}
          >
            Edit
          </Button>
          <Button
            variant="outline-danger"
            size="sm"
            className="action-btn"
            onClick={() => handleDelete(row.original)}
            disabled={row.original.isDefault}
            title={row.original.isDefault ? "Cannot delete the default method" : "Delete Method"}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ], [handleDelete, handleSetDefault, handleImageClick]);
  
  const isSubmitting = createMethodMutation.isPending || updateMethodMutation.isPending;

  return (
    <>
      <MainHeader />
      <PageHeader
        title="Payment Methods"
        subtitle="Manage QR code payment options for users"
      />

      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control
              placeholder="Search methods..."
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
          <BsPlusCircleFill /> Add New Method
        </Button>
      </TableControls>

      <ReusableTable
        columns={columns}
        data={methods}
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

      <PaymentMethodFormModal
        show={modalState.show}
        onHide={handleCloseModal}
        method={modalState.currentItem}
        onSubmit={handleFormSubmit}
        isSubmitting={isSubmitting}
      />
      
      <ImageModal
        show={!!imageModalUrl}
        onHide={() => setImageModalUrl(null)}
        imageUrl={imageModalUrl}
        altText="Fullscreen QR code"
      />
    </>
  );
}

export default PaymentManagementPage;