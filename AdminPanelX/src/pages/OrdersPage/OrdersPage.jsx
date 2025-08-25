import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  fetchAllOrders,
  fetchAllOrderStatuses,
} from '../../services/OrderService';
import { format } from 'date-fns';
import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import TableControls from '../../components/TableControls/TableControls';
import { Alert, Spinner, Button, Form, InputGroup } from 'react-bootstrap';
import { BsSearch, BsArrowCounterclockwise } from 'react-icons/bs';
import './OrdersPage.css';

function OrdersPage() {
  const navigate = useNavigate();
  const { token } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();

  // ใช้ useMemo ในการอ่านค่า state ของตารางจาก URL search params
  // ทำให้ URL เป็น 'source of truth' และสามารถแชร์ลิงก์พร้อม filter/sort ที่ตั้งไว้ได้
  const tableState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams
      ? JSON.parse(sortingParams)
      : [{ id: 'createdAt', desc: true }]; // ค่า default คือเรียงตามวันที่สร้างล่าสุด
    const globalFilter = searchParams.get('globalFilter') || '';
    const statusFilter = searchParams.get('statusFilter');
    const customerFilter = searchParams.get('customerFilter');
    const columnFilters = [];
    if (statusFilter)
      columnFilters.push({ id: 'orderStatus', value: statusFilter });
    if (customerFilter)
      columnFilters.push({ id: 'email', value: customerFilter });

    return {
      pagination: { pageIndex, pageSize },
      sorting,
      globalFilter,
      columnFilters,
    };
  }, [searchParams]);

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [allStatuses, setAllStatuses] = useState([]);

  // state ของตาราง (pagination, sorting, etc.) จะถูก sync กับ URL
  const [pagination, setPagination] = useState(tableState.pagination);
  const [sorting, setSorting] = useState(tableState.sorting);
  const [globalFilter, setGlobalFilter] = useState(tableState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(tableState.columnFilters);

  const loadData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      const [ordersData, statusesData] = await Promise.all([
        fetchAllOrders(token),
        fetchAllOrderStatuses(token),
        new Promise((resolve) => setTimeout(resolve, 20)),
      ]);
      setOrders(ordersData);
      setAllStatuses(statusesData);
    } catch (err) {
      setError(err.message || 'Failed to fetch data.');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // effect นี้จะคอยจับการเปลี่ยนแปลงของ state ตาราง แล้วอัปเดต URL search params ตาม
  useEffect(() => {
    const newSearchParams = new URLSearchParams();
    if (pagination.pageIndex > 0)
      newSearchParams.set('page', pagination.pageIndex.toString());
    if (pagination.pageSize !== 10)
      newSearchParams.set('pageSize', pagination.pageSize.toString());
    // ไม่ต้องเซ็ต sort ใน URL ถ้าเป็นค่า default
    if (sorting && (sorting[0]?.id !== 'createdAt' || !sorting[0]?.desc)) {
      newSearchParams.set('sort', JSON.stringify(sorting));
    }
    if (globalFilter) newSearchParams.set('globalFilter', globalFilter);
    const statusFilter = columnFilters.find(
      (f) => f.id === 'orderStatus'
    )?.value;
    const customerFilter = columnFilters.find((f) => f.id === 'email')?.value;
    if (statusFilter) newSearchParams.set('statusFilter', statusFilter);
    if (customerFilter) newSearchParams.set('customerFilter', customerFilter);

    // ใช้ replace: true เพื่อไม่ให้ history ของ browserรก
    setSearchParams(newSearchParams, { replace: true });
  }, [pagination, sorting, globalFilter, columnFilters, setSearchParams]);

  // สร้าง list ของ customer ที่ไม่ซ้ำกันสำหรับ dropdown filter
  const uniqueCustomers = useMemo(() => {
    if (orders.length === 0) return [];
    return [...new Set(orders.map((order) => order.email))].sort();
  }, [orders]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([{ id: 'createdAt', desc: true }]);
    loadData();
  };

  // ฟังก์ชัน helper สำหรับการตั้งค่า column filter
  const setFilter = (columnId, value) => {
    setColumnFilters((prev) => {
      const newFilters = prev.filter((f) => f.id !== columnId);
      if (value) newFilters.push({ id: columnId, value });
      return newFilters;
    });
  };

  // กำหนด columns ของตารางโดยใช้ useMemo เพื่อ performance
  const columns = useMemo(
    () => [
      {
        accessorKey: 'id',
        header: 'Order ID',
        meta: { width: '15%' },
        cell: (info) => (
          // แสดงแค่ 8 ตัวท้าย แต่ให้ดูตัวเต็มได้ตอนเอาเมาส์ชี้
          <span className="order-id" title={info.getValue()}>
            {info.getValue().slice(-8)}
          </span>
        ),
      },
      { accessorKey: 'email', header: 'Customer', meta: { width: '25%' } },
      {
        accessorKey: 'createdAt',
        header: 'Date',
        meta: { width: '20%' },
        cell: (info) => format(new Date(info.getValue()), 'dd MMM yyyy, HH:mm'),
      },
      {
        accessorKey: 'totalAmount',
        header: 'Total',
        meta: { width: '15%' },
        cell: (info) =>
          `฿ ${Number(info.getValue()).toLocaleString(undefined, {
            minimumFractionDigits: 2,
          })}`,
      },
      {
        accessorKey: 'paymentStatus',
        header: 'Payment',
        meta: { width: '15%' },
        cell: (info) => <StatusBadge status={info.getValue()} type="payment" />,
      },
      {
        accessorKey: 'orderStatus',
        header: 'Order Status',
        meta: { width: '15%' },
        cell: (info) => <StatusBadge status={info.getValue()} type="order" />,
      },
      {
        id: 'actions',
        header: 'Actions',
        meta: { cellClassName: 'text-center-cell', width: '150px' },
        cell: ({ row }) => (
          <div className="d-flex gap-2 justify-content-center">
            <Button
              variant="outline-primary"
              size="sm"
              className="action-btn"
              onClick={() =>
                navigate(`/order-details/${row.original.id}`, {
                  state: { from: location },
                })
              }
            >
              View Details
            </Button>
          </div>
        ),
      },
    ],
    [navigate, location]
  );

  const statusFilterValue =
    columnFilters.find((f) => f.id === 'orderStatus')?.value || '';
  const customerFilterValue =
    columnFilters.find((f) => f.id === 'email')?.value || '';

  return (
    <>
      <MainHeader />
      <PageHeader
        title="Order Management"
        subtitle="View, search, and manage customer orders"
      />

      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control
              placeholder="Search all orders..."
              value={globalFilter ?? ''}
              onChange={(e) => setGlobalFilter(e.target.value)}
              className="search-input"
            />
            <InputGroup.Text className="search-input-group-text">
              <BsSearch />
            </InputGroup.Text>
          </InputGroup>

          <Form.Select
            className="type-filter customer-filter"
            aria-label="Filter by customer"
            value={customerFilterValue}
            onChange={(e) => setFilter('email', e.target.value)}
          >
            <option value="">All Customers</option>
            {uniqueCustomers.map((email) => (
              <option key={email} value={email}>
                {email}
              </option>
            ))}
          </Form.Select>

          <Form.Select
            className="type-filter"
            aria-label="Filter by order status"
            value={statusFilterValue}
            onChange={(e) => setFilter('orderStatus', e.target.value)}
          >
            <option value="">All Statuses</option>
            {allStatuses.map((status) => (
              <option key={status} value={status}>
                {status.replace(/_/g, ' ')}
              </option>
            ))}
          </Form.Select>

          <Button
            variant="outline-secondary"
            onClick={handleResetFilters}
            className="d-flex align-items-center gap-1"
          >
            <BsArrowCounterclockwise /> Reset
          </Button>
        </div>
      </TableControls>

      <ReusableTable
        columns={columns}
        data={orders}
        isLoading={loading}
        error={error}
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
    </>
  );
}

export default OrdersPage;