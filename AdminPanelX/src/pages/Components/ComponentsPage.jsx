import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchAllComponents, deleteComponent, updateComponentStock } from '../../services/ComponentService';
import { notifySuccess, notifyError } from '../../services/NotificationService';
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
  const [searchParams, setSearchParams] = useSearchParams();

  const initialState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams ? JSON.parse(sortingParams) : [];
    const globalFilter = searchParams.get('globalFilter') || '';
    const typeFilter = searchParams.get('typeFilter') || '';
    const columnFilters = typeFilter ? [{ id: 'type', value: typeFilter }] : [];
    return { pagination: { pageIndex, pageSize }, sorting, globalFilter, columnFilters };
  }, [searchParams]);

  const [components, setComponents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sorting, setSorting] = useState(initialState.sorting);
  const [globalFilter, setGlobalFilter] = useState(initialState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(initialState.columnFilters);
  const [pagination, setPagination] = useState(initialState.pagination);
  const [editingRowId, setEditingRowId] = useState(null);
  const [imageModalUrl, setImageModalUrl] = useState(null);

  const loadData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      const [data] = await Promise.all([
        fetchAllComponents(token),
        new Promise((resolve) => setTimeout(resolve, 20)),
      ]);
      setComponents(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => { loadData(); }, [loadData]);

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

  const uniqueTypes = useMemo(() => {
    if (components.length === 0) return [];
    return Array.from(new Set(components.map((c) => c.type).filter(Boolean))).sort();
  }, [components]);

  const handleImageClick = useCallback((imageUrl) => { setImageModalUrl(imageUrl); }, []);

  const handleDelete = useCallback(async (component) => {
    try {
      if (await deleteComponent(component, token)) {
        setComponents((prev) => prev.filter((c) => c.id !== component.id));
      }
    } catch (err) {
      console.error('Deletion process failed:', err);
    }
  }, [token]);

  const handleStockUpdate = useCallback(async (row, newStockValueStr) => {
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
    try {
      const updated = await updateComponentStock(component.id, quantityChange, token);
      setComponents((p) => p.map((c) => (c.id === updated.id ? updated : c)));
    } catch (err) {
      console.error('Stock update failed:', err);
    } finally {
      setEditingRowId(null);
    }
  }, [token]);

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
        accessorKey: 'imageUrl', header: 'Image', enableSorting: false, meta: { cellClassName: 'text-center-cell', width: '80px' },
        cell: ({ row }) => {
          const imageUrl = row.original.imageUrl;
          return imageUrl ? (
            <img src={imageUrl} alt={row.original.name} style={{ width: '50px', height: '50px', objectFit: 'cover', borderRadius: '8px' }} onClick={() => handleImageClick(imageUrl)} className="clickable-image" />
          ) : 'N/A';
        },
      },
      {
        accessorKey: 'name', header: 'Name',
        cell: (info) => <TruncatedText text={info.getValue()} />,
      },
      {
        accessorKey: 'mpn', header: 'MPN',
        cell: (info) => <TruncatedText text={info.getValue()} />,
      },
      { accessorKey: 'type', header: 'Type', meta: { width: '140px' } },
      {
        accessorKey: 'price', header: 'Price', meta: { width: '130px' },
        cell: (info) => `฿ ${Number(info.getValue()).toFixed(2)}`,
      },
      {
        accessorKey: 'quantity', header: 'Stock', meta: { cellClassName: 'text-center-cell', width: '145px' },
        cell: ({ row }) => {
          const component = row.original;
          if (editingRowId === row.id) {
            return (
              <InlineStockEditor
                initialValue={component.quantity}
                onSave={(newValue) => handleStockUpdate(row, newValue)}
                onCancel={() => setEditingRowId(null)}
              />
            );
          }
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
    ],
    [editingRowId, navigate, location, handleImageClick, handleDelete, handleStockUpdate]
  );

  const typeFilterValue = columnFilters.find((f) => f.id === 'type')?.value || '';

  return (
    <>
      <MainHeader />
      <PageHeader title="Manage Components" subtitle="View, search, and manage product components" />
      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control placeholder="Search all components..." value={globalFilter ?? ''} onChange={(e) => setGlobalFilter(e.target.value)} className="search-input" />
            <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
          </InputGroup>
          <Form.Select className="type-filter" aria-label="Filter by component type" value={typeFilterValue}
            onChange={(e) => {
              const value = e.target.value;
              setColumnFilters((prev) => prev.filter((f) => f.id !== 'type').concat(value ? [{ id: 'type', value }] : []));
            }}
          >
            <option value="">All Types</option>
            {uniqueTypes.map((type) => (<option key={type} value={type}>{type}</option>))}
          </Form.Select>
          <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
            <BsArrowCounterclockwise /> Reset
          </Button>
        </div>
        <Button variant="primary" size="lg" className="d-flex align-items-center gap-2" onClick={() => navigate('/add-component')}>
          <BsPlusCircleFill /> Add New Component
        </Button>
      </TableControls>

      <ReusableTable
        columns={columns}
        data={components}
        isLoading={loading}
        error={error && !components.length ? error : null}
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

      <ImageModal show={!!imageModalUrl} onHide={() => setImageModalUrl(null)} imageUrl={imageModalUrl} altText="Fullscreen component view" />
    </>
  );
}

export default ComponentsPage;