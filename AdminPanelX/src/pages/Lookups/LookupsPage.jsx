import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import {
  fetchLookupsByType,
  createLookup,
  updateLookup,
  deleteLookup,
  createBrand,
  updateBrand,
  deleteBrand,
} from '../../services/LookupService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';

import PageHeader from '../../components/PageHeader/PageHeader';
import MainHeader from '../../components/MainHeader/MainHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';
import ImageModal from '../../components/ImageModal/ImageModal';
import TableControls from '../../components/TableControls/TableControls';
import LookupFormModal from '../../components/LookupFormModal/LookupFormModal';

import { Tabs, Tab, Button, Form, InputGroup, Image } from 'react-bootstrap';
import { BsPlusCircleFill, BsSearch, BsArrowCounterclockwise } from 'react-icons/bs';

import './LookupsPage.css';
import '../../components/ImageModal/ImageModal.css';

// Config หลัก: เพิ่ม/แก้ไขแท็บและคอลัมน์ที่นี่
const lookupConfig = {
  sockets: { title: 'Sockets', columns: ['name', 'brand'], fields: ['name', 'brand'] },
  'ram-types': { title: 'RAM Types', columns: ['name'], fields: ['name'] },
  'form-factors': { title: 'Form Factors', columns: ['name', 'type'], fields: ['name', 'type'] },
  'storage-interfaces': { title: 'Storage Interfaces', columns: ['name'], fields: ['name'] },
  brands: { title: 'Brands', columns: ['logoUrl', 'name'], fields: ['name'], hasImage: true },
};

// Config filter สำหรับ Form Factor
const formFactorTypes = ['MOTHERBOARD', 'PSU', 'STORAGE'];

function LookupsPage() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  const activeTab = useMemo(() => searchParams.get('tab') || 'sockets', [searchParams]);

  // อ่าน state ตารางจาก URL
  const tableState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams ? JSON.parse(sortingParams) : [];
    const globalFilter = searchParams.get('globalFilter') || '';
    const typeFilter = searchParams.get('typeFilter') || '';
    const columnFilters = typeFilter ? [{ id: 'type', value: typeFilter }] : [];

    return { pagination: { pageIndex, pageSize }, sorting, globalFilter, columnFilters };
  }, [searchParams]);

  // ดึงข้อมูลตามแท็บที่เลือก
  const { data: lookups = [], isLoading, isFetching, error, refetch } = useQuery({
    queryKey: [activeTab],
    queryFn: async () => {
        const [data] = await Promise.all([
            fetchLookupsByType(activeTab, token),
            new Promise((resolve) => setTimeout(resolve, 20)),
        ]);
        return data;
    },
    enabled: !!token,
  });

  // Helper สำหรับสร้าง CUD mutations
  const useLookupMutation = (mutationFn, action) => {
    return useMutation({
        mutationFn,
        onSuccess: () => {
            const singularTitle = lookupConfig[activeTab].title.slice(0, -1);
            notifySuccess(`${singularTitle} ${action} successfully!`);
            queryClient.invalidateQueries({ queryKey: [activeTab] });
            handleCloseModal();
        },
        onError: (err) => notifyError(err.message),
    });
  };
  
  // เช็ค hasImage เพื่อเรียก service ที่ถูกต้อง
  const createItemMutation = useLookupMutation(
    (vars) => {
      const { data, imageFile } = vars;
      return lookupConfig[activeTab].hasImage
        ? createBrand(data, imageFile, token)
        : createLookup(activeTab, data, token);
    },
    'created'
  );

  const updateItemMutation = useLookupMutation(
    (vars) => {
      const { id, data, imageFile } = vars;
      return lookupConfig[activeTab].hasImage
        ? updateBrand(id, data, imageFile, token)
        : updateLookup(activeTab, id, data, token);
    },
    'updated'
  );

  const deleteItemMutation = useLookupMutation(
    (item) => {
      return lookupConfig[activeTab].hasImage
        ? deleteBrand(item.id, token)
        : deleteLookup(activeTab, item.id, token);
    },
    'deleted'
  );

  const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });
  const [imageModalUrl, setImageModalUrl] = useState(null);
  
  const [pagination, setPagination] = useState(tableState.pagination);
  const [sorting, setSorting] = useState(tableState.sorting);
  const [globalFilter, setGlobalFilter] = useState(tableState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(tableState.columnFilters);
  
  // อัปเดต URL search params เมื่อ state ตารางเปลี่ยน
  useEffect(() => {
    const newSearchParams = new URLSearchParams();
    newSearchParams.set('tab', activeTab);
    
    if (pagination.pageIndex > 0) newSearchParams.set('page', pagination.pageIndex.toString());
    if (pagination.pageSize !== 10) newSearchParams.set('pageSize', pagination.pageSize.toString());
    if (sorting.length > 0) newSearchParams.set('sort', JSON.stringify(sorting));
    if (globalFilter) newSearchParams.set('globalFilter', globalFilter);
    
    const typeFilter = columnFilters.find((f) => f.id === 'type')?.value;
    if (typeFilter) newSearchParams.set('typeFilter', typeFilter);
    
    setSearchParams(newSearchParams, { replace: true });
  }, [activeTab, pagination, sorting, globalFilter, columnFilters, setSearchParams]);

  const handleImageClick = useCallback((imageUrl) => setImageModalUrl(imageUrl), []);
  const handleTabSelect = (tabKey) => setSearchParams({ tab: tabKey });
  const handleShowModal = (type, item = null) => setModalState({ show: true, type, currentItem: item });
  const handleCloseModal = () => setModalState({ show: false, type: 'add', currentItem: null });

  const handleFormSubmit = async (formData, imageFile) => {
    const { type, currentItem } = modalState;
    const data = Object.fromEntries(formData.entries());

    if (type === 'add') {
      createItemMutation.mutate({ data, imageFile });
    } else {
      updateItemMutation.mutate({ id: currentItem.id, data, imageFile });
    }
  };

  const handleDelete = useCallback(async (item) => {
    const confirmed = await showConfirmation('Are you sure?', `This will permanently delete "${item.name}".`);
    if (confirmed) {
      deleteItemMutation.mutate(item);
    }
  }, [deleteItemMutation]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([]);
    refetch();
  };

  // สร้างคอลัมน์ตารางตาม config ของแต่ละแท็บ
  const columns = useMemo(() => {
    const config = lookupConfig[activeTab];
    if (!config) return [];

    const baseColumns = config.columns.map((key) => {
      if (key === 'logoUrl') {
        return {
          accessorKey: 'logoUrl',
          header: 'Logo',
          enableSorting: false,
          meta: { width: '45%' },
          cell: ({ row }) => {
            const imageUrl = row.original.logoUrl;
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
        };
      }
      return {
        accessorKey: key,
        header: key.charAt(0).toUpperCase() + key.slice(1)
      };
    });

    baseColumns.push({
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
    });

    return baseColumns;
  }, [activeTab, handleDelete, handleImageClick]);

  const typeFilterValue = columnFilters.find((f) => f.id === 'type')?.value || '';
  const isSubmitting = createItemMutation.isPending || updateItemMutation.isPending;

  return (
    <>
      <MainHeader />
      <PageHeader
        title="Manage Lookups"
        subtitle="Add, edit, or delete data used in component forms"
      />

      <Tabs
        id="lookups-tabs"
        activeKey={activeTab}
        onSelect={handleTabSelect}
        className="mb-3 lookups-tabs"
      >
         {Object.keys(lookupConfig).map((key) => (
          <Tab eventKey={key} title={lookupConfig[key].title} key={key}>

            <TableControls>
              <div className="filter-controls">
                <InputGroup className="search-bar">
                  <Form.Control
                    placeholder={`Search ${lookupConfig[key].title}...`}
                    value={globalFilter ?? ''}
                    onChange={(e) => setGlobalFilter(e.target.value)}
                    className="search-input"
                  />
                  <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
                </InputGroup>

                {/* แสดง dropdown filter เฉพาะแท็บ 'form-factors' */}
                {key === 'form-factors' && (
                  <Form.Select
                    className="type-filter"
                    aria-label="Filter by form factor type"
                    value={typeFilterValue}
                    onChange={(e) => {
                      const value = e.target.value;
                      const newFilters = (prev) => prev.filter((f) => f.id !== 'type').concat(value ? [{ id: 'type', value }] : []);
                      setColumnFilters(newFilters);
                    }}
                  >
                    <option value="">All Types</option>
                    {formFactorTypes.map((type) => (<option key={type} value={type}>{type}</option>))}
                  </Form.Select>
                )}

                <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
                  <BsArrowCounterclockwise /> Reset
                </Button>
              </div>

              <Button variant="primary" onClick={() => handleShowModal('add')} className="d-flex align-items-center gap-2">
                <BsPlusCircleFill /> Add New {lookupConfig[key].title.slice(0, -1)}
              </Button>
            </TableControls>

            <ReusableTable
              columns={columns}
              data={lookups}
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

          </Tab>
        ))}
      </Tabs>

      <LookupFormModal
        show={modalState.show}
        onHide={handleCloseModal}
        onSubmit={handleFormSubmit}
        isSubmitting={isSubmitting}
        modalState={modalState}
        activeTab={activeTab}
        lookupConfig={lookupConfig}
        formFactorTypes={formFactorTypes}
      />

      <ImageModal
        show={!!imageModalUrl}
        onHide={() => setImageModalUrl(null)}
        imageUrl={imageModalUrl}
        altText="Fullscreen brand logo"
      />
    </>
  );
}

export default LookupsPage;