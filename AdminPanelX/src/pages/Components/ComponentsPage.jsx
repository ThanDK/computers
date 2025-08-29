import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import {
    fetchAllComponents,
    deleteComponent,
    updateComponentStock
} from '../../services/ComponentService';
import { notifySuccess, notifyError, handlePromise } from '../../services/NotificationService';

import PageHeader from '../../components/PageHeader/PageHeader';
import MainHeader from '../../components/MainHeader/MainHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';
import ImageModal from '../../components/ImageModal/ImageModal';
import TableControls from '../../components/TableControls/TableControls';
import TruncatedText from '../../components/TruncatedText/TruncatedText';
import InlineStockEditor from '../../components/InlineStockEditor/InlineStockEditor';

import { Button, Form, InputGroup } from 'react-bootstrap';
import { BsSearch, BsPlusCircleFill, BsArrowCounterclockwise } from 'react-icons/bs';

import './ComponentsPage.css';
import '../../components/ImageModal/ImageModal.css';

function ComponentsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  // อ่าน state ตารางจาก URL, ทำให้แชร์ลิงก์พร้อม filter/sort ได้
  const initialState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams ? JSON.parse(sortingParams) : [];
    const globalFilter = searchParams.get('globalFilter') || '';
    const typeFilter = searchParams.get('typeFilter') || '';
    const columnFilters = typeFilter ? [{ id: 'type', value: typeFilter }] : [];

    return {
        pagination: { pageIndex, pageSize },
        sorting,
        globalFilter,
        columnFilters
    };
  }, [searchParams]);

  const [sorting, setSorting] = useState(initialState.sorting);
  const [globalFilter, setGlobalFilter] = useState(initialState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(initialState.columnFilters);
  const [pagination, setPagination] = useState(initialState.pagination);
  const [editingRowId, setEditingRowId] = useState(null);
  const [imageModalUrl, setImageModalUrl] = useState(null);

  // ดึงข้อมูล components ทั้งหมด (ใส่ setTimeout เล็กน้อยกัน loading กระพริบเร็วไป)
  const { data: components = [], isLoading, isFetching, error, refetch } = useQuery({
    queryKey: ['components'],
    queryFn: async () => {
        const [data] = await Promise.all([
            fetchAllComponents(token),
            new Promise((resolve) => setTimeout(resolve, 20)),
        ]);
        return data;
    },
    enabled: !!token,
  });

  // จัดการอัปเดตสต็อกแบบ Optimistic Update
  const updateStockMutation = useMutation({
    mutationFn: (variables) => updateComponentStock(variables.componentId, variables.quantityChange, token),
    // onMutate จะทำงานก่อน mutationFn: ที่นี่จะอัปเดต UI ไปก่อนเลย
    onMutate: async (variables) => {
        await queryClient.cancelQueries({ queryKey: ['components'] });
        // เก็บข้อมูลเก่าไว้ เผื่อ rollback ตอน error
        const previousComponents = queryClient.getQueryData(['components']);
        const newStock = variables.originalStock + variables.quantityChange;
        
        // อัปเดต UI ทันที โดยไม่ต้องรอ server ตอบกลับ
        queryClient.setQueryData(['components'], (old) =>
          old.map((component) =>
            component.id === variables.componentId ? { ...component, quantity: newStock } : component
          )
        );
        return { previousComponents };
    },
    // ถ้าสำเร็จ, แจ้งเตือนและอัปเดต cache ด้วยข้อมูลใหม่จริงๆ จาก server
    onSuccess: (updatedComponent) => {
        notifySuccess(`Stock for "${updatedComponent.name}" updated!`);
        queryClient.setQueryData(['components'], (oldData) =>
            oldData.map((c) => (c.id === updatedComponent.id ? updatedComponent : c))
        );
    },
    // ถ้า error, rollback UI กลับไปใช้ข้อมูลเก่าที่เก็บไว้
    onError: (err, variables, context) => {
        notifyError(err.message);
        queryClient.setQueryData(['components'], context.previousComponents);
    },
    // ไม่ว่าจะสำเร็จหรือล้มเหลว, ปิดโหมดแก้ไขเสมอ
    onSettled: () => {
        setEditingRowId(null);
    },
  });

  // จัดการลบ component แบบ Optimistic Update
  const deleteComponentMutation = useMutation({
    mutationFn: (component) => deleteComponent(component, token),
    // onMutate: ลบ item ออกจาก UI ไปก่อนเลย
    onMutate: async (componentToDelete) => {
        await queryClient.cancelQueries({ queryKey: ['components'] });
        const previousComponents = queryClient.getQueryData(['components']);
        queryClient.setQueryData(['components'], (old) =>
          old.filter((component) => component.id !== componentToDelete.id)
        );
        return { previousComponents };
    },
    onSuccess: (result, component) => {
        notifySuccess(`"${component.name}" deleted successfully.`);
    },
    onError: (err, variables, context) => {
        if (err.message !== 'Deletion cancelled by user.') {
            notifyError(err.message);
        }
        // ถ้า error, rollback โดยเอาข้อมูลที่ลบไปกลับมาใส่ใน UI
        queryClient.setQueryData(['components'], context.previousComponents);
    },
  });
  
  // อัปเดต URL search params เมื่อ state ตารางเปลี่ยน
  useEffect(() => {
    const newSearchParams = new URLSearchParams();

    if (pagination.pageIndex > 0) newSearchParams.set('page', pagination.pageIndex);
    if (pagination.pageSize !== 10) newSearchParams.set('pageSize', pagination.pageSize);
    if (sorting.length > 0) newSearchParams.set('sort', JSON.stringify(sorting));
    if (globalFilter) newSearchParams.set('globalFilter', globalFilter);
    
    const typeFilter = columnFilters.find((f) => f.id === 'type')?.value;
    if (typeFilter) newSearchParams.set('typeFilter', typeFilter);

    setSearchParams(newSearchParams, { replace: true });
  }, [pagination, sorting, globalFilter, columnFilters, setSearchParams]);

  // สร้าง list 'type' ที่ไม่ซ้ำสำหรับ dropdown
  const uniqueTypes = useMemo(() => {
    if (components.length === 0) return [];
    return Array.from(new Set(components.map((c) => c.type).filter(Boolean))).sort();
  }, [components]);

  const handleImageClick = useCallback((imageUrl) => { setImageModalUrl(imageUrl); }, []);
  const handleDelete = useCallback((component) => { deleteComponentMutation.mutate(component); }, [deleteComponentMutation]);

  // จัดการการอัปเดตสต็อก, คำนวณส่วนต่างแล้วเรียก mutation
  const handleStockUpdate = useCallback((row, newStockValueStr) => {
    const newStockValue = Number(newStockValueStr);
    const component = row.original;
    const quantityChange = newStockValue - component.quantity;

    if (isNaN(newStockValue) || newStockValue < 0) {
      notifyError('Invalid stock value.');
      return;
    }
    if (quantityChange === 0) {
      setEditingRowId(null);
      return;
    }
    
    updateStockMutation.mutate({
        componentId: component.id,
        quantityChange: quantityChange,
        originalStock: component.quantity,
    });
  }, [updateStockMutation]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([]);
    refetch();
  };

  // Config คอลัมน์สำหรับตาราง, ใช้ useMemo เพื่อไม่ให้สร้างใหม่ทุกครั้งที่ re-render
  const columns = useMemo(() => [
    {
      accessorKey: 'imageUrl', header: 'Image', enableSorting: false, meta: { cellClassName: 'text-center-cell', width: '80px' },
      cell: ({ row }) => {
        const imageUrl = row.original.imageUrl;
        return imageUrl ? (
          <img src={imageUrl} alt={row.original.name} style={{ width: '50px', height: '50px', objectFit: 'cover', borderRadius: '8px' }} onClick={() => handleImageClick(imageUrl)} className="clickable-image" />
        ) : 'N/A';
      },
    },
    { accessorKey: 'name', header: 'Name', cell: (info) => <TruncatedText text={info.getValue()} /> },
    { accessorKey: 'mpn', header: 'MPN', cell: (info) => <TruncatedText text={info.getValue()} /> },
    { accessorKey: 'type', header: 'Type', meta: { width: '140px' } },
    { accessorKey: 'price', header: 'Price', meta: { width: '130px' }, cell: (info) => `฿ ${Number(info.getValue()).toFixed(2)}` },
    {
      accessorKey: 'quantity', header: 'Stock', meta: { cellClassName: 'text-center-cell', width: '145px' },
      cell: ({ row }) => {
        const component = row.original;
        // ถ้าแถวนี้อยู่ในโหมดแก้ไข, แสดง InlineStockEditor
        if (editingRowId === row.id) {
          return (
            <InlineStockEditor
              initialValue={component.quantity}
              onSave={(newValue) => handleStockUpdate(row, newValue)}
              onCancel={() => setEditingRowId(null)}
            />
          );
        }
        // ปกติจะแสดงค่าสต็อก, double-click เพื่อแก้ไข
        return (
          <div className="status-dot-container" title="Double-click to edit stock" onDoubleClick={(e) => { e.stopPropagation(); setEditingRowId(row.id); }}>
            <div className={`status-dot ${component.quantity > 0 ? 'status-dot-active' : 'status-dot-inactive'}`}></div>
            <span>{component.quantity}</span>
          </div>
        );
      },
    },
    {
      id: 'actions', header: 'Actions', meta: { cellClassName: 'text-center-cell', width: '150px' },
      cell: ({ row }) => (
        <div className="d-flex gap-2 justify-content-center">
          <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => navigate(`/edit-component/${row.original.id}`, { state: { from: location } })}>
            Edit
          </Button>
          <Button variant="outline-danger" size="sm" className="action-btn action-btn-delete" onClick={() => handleDelete(row.original)}>
            Delete
          </Button>
        </div>
      ),
    },
  ], [editingRowId, navigate, location, handleImageClick, handleDelete, handleStockUpdate]);

  const typeFilterValue = columnFilters.find((f) => f.id === 'type')?.value || '';

  return (
    <>
      <MainHeader />
      <PageHeader
        title="Manage Components"
        subtitle="View, search, and manage product components"
      />

      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control
              placeholder="Search all components..."
              value={globalFilter ?? ''}
              onChange={(e) => setGlobalFilter(e.target.value)}
              className="search-input"
            />
            <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
          </InputGroup>

          <Form.Select
            className="type-filter"
            aria-label="Filter by component type"
            value={typeFilterValue}
            onChange={(e) => {
              const value = e.target.value;
              const newFilters = (prev) => prev.filter((f) => f.id !== 'type').concat(value ? [{ id: 'type', value }] : []);
              setColumnFilters(newFilters);
            }}
          >
            <option value="">All Types</option>
            {uniqueTypes.map((type) => (<option key={type} value={type}>{type}</option>))}
          </Form.Select>

          <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
            <BsArrowCounterclockwise /> Reset
          </Button>
        </div>

        <Button
          variant="primary"
          size="lg"
          className="d-flex align-items-center gap-2"
          onClick={() => navigate('/add-component')}
        >
          <BsPlusCircleFill /> Add New Component
        </Button>
      </TableControls>

      <ReusableTable
        columns={columns}
        data={components}
        isLoading={isLoading || isFetching}
        error={error && !components.length ? error : null}
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

      <ImageModal
        show={!!imageModalUrl}
        onHide={() => setImageModalUrl(null)}
        imageUrl={imageModalUrl}
        altText="Fullscreen component view"
      />
    </>
  );
}

export default ComponentsPage;