import React, { useState, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Tab, Tabs, Button, Spinner, ButtonGroup, Popover, OverlayTrigger, Dropdown } from 'react-bootstrap';
import { subDays, format, differenceInDays } from 'date-fns';
import { BsTable, BsBarChart, BsDownload, BsCalendarEvent } from 'react-icons/bs';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import { CustomDatePicker } from '../../components/DatePicker/CustomDatePicker';
import ReportTable from '../../components/ReportTable/ReportTable';
import OrderFilters from '../../components/ReportFilters/OrderFilters';
import ReportLineChart from '../../components/Charts/ReportLineChart';
import ReportBarChart from '../../components/Charts/ReportBarChart';
import ReportGroupedBarChart from '../../components/Charts/ReportGroupedBarChart';
import ReportControlsHeader from '../../components/ReportControlsHeader/ReportControlsHeader';

import { useAuth } from '../../context/AuthContext';
import { searchOrders, fetchTopSellingReport, fetchLowStockReport, downloadReportAsCsv } from '../../services/ReportService';
import { API_BASE_URL } from '../../services/apiConfig';

import './ReportsPage.css';

const orderColumns = [
    { header: 'Order ID', accessorKey: 'orderId' },
    { header: 'Date', accessorKey: 'orderDate', cell: info => format(new Date(info.getValue()), 'MMM d, yyyy') },
    { header: 'Customer', accessorKey: 'customerName' },
    { header: 'Order Status', accessorKey: 'orderStatus' },
    { header: 'Payment Status', accessorKey: 'paymentStatus' },
    { header: 'Payment Method', accessorKey: 'paymentMethod' },
    { header: 'Total', accessorKey: 'totalAmount', cell: info => `฿${info.getValue().toLocaleString('en-US')}` },
];
const topSellingColumns = [
    { header: 'Product Name', accessorKey: 'productName' },
    { header: 'MPN', accessorKey: 'mpn' },
    { header: 'Quantity Sold', accessorKey: 'totalQuantitySold' },
    { header: 'Total Revenue', accessorKey: 'totalRevenueGenerated', cell: info => `฿${info.getValue().toLocaleString('en-US')}` },
];
const lowStockColumns = [
    { header: 'Product Name', accessorKey: 'componentName' },
    { header: 'MPN', accessorKey: 'mpn' },
    { header: 'Stock Remaining', accessorKey: 'quantityRemaining' },
];

const transformFiltersForApi = (filters) => {
    return filters
        .filter(f => {
            if (Array.isArray(f.value)) return f.value.length > 0;
            return f.value;
        })
        .map(f => ({
            field: f.field,
            operator: f.operator,
            values: Array.isArray(f.value) ? f.value : [f.value],
        }));
};

const ReportsPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = useAuth();
  
  const fromLocation = location.state?.from?.pathname || '/dashboard';
  
  const [activeTab, setActiveTab] = useState('orders');
  const [viewMode, setViewMode] = useState('table');
  const [chartType, setChartType] = useState('revenueLine');
  const [dateRange, setDateRange] = useState({ from: subDays(new Date(), 29), to: new Date() });
  const [filters, setFilters] = useState([]);
  const [filterLogic, setFilterLogic] = useState('AND');
  const [showPicker, setShowPicker] = useState(false);

  const { data: orderData, isLoading: isOrdersLoading, isError: isOrdersError } = useQuery({
    queryKey: ['ordersReport', dateRange, filters, filterLogic],
    queryFn: () => {
        const requestBody = {
            startDate: dateRange.from,
            endDate: dateRange.to,
            logic: filterLogic,
            filters: transformFiltersForApi(filters),
        };
        return searchOrders(token, requestBody);
    },
    enabled: !!token && activeTab === 'orders',
  });

  const { data: topSellingData, isLoading: isTopSellingLoading, isError: isTopSellingError } = useQuery({
    queryKey: ['topSellingReport', dateRange],
    queryFn: () => fetchTopSellingReport(token, dateRange.from, dateRange.to),
    enabled: !!token && activeTab === 'top-selling',
  });

  const { data: lowStockData, isLoading: isLowStockLoading, isError: isLowStockError } = useQuery({
    queryKey: ['lowStockReport'],
    queryFn: () => fetchLowStockReport(token),
    enabled: !!token && activeTab === 'low-stock',
  });

  const handleTabSelect = (key) => {
    setActiveTab(key);
    setViewMode('table');
    if (key === 'orders') setChartType('revenueLine'); 
  };
  
  const handleDownloadCsv = async () => {
    try {
        if (activeTab === 'orders') {
            const requestBody = {
                startDate: dateRange.from,
                endDate: dateRange.to,
                logic: filterLogic,
                filters: transformFiltersForApi(filters),
            };
            await downloadReportAsCsv(token, `${API_BASE_URL}/reports/orders/search`, 'order-report', requestBody);
        } else if (activeTab === 'top-selling') {
            const url = `${API_BASE_URL}/reports/products/top-selling?startDate=${format(dateRange.from, 'yyyy-MM-dd')}&endDate=${format(dateRange.to, 'yyyy-MM-dd')}`;
            await downloadReportAsCsv(token, url, 'top-selling-report');
        } else if (activeTab === 'low-stock') {
            await downloadReportAsCsv(token, `${API_BASE_URL}/reports/stock/low`, 'low-stock-report');
        }
    } catch (error) {
        console.error("Failed to download CSV:", error);
    }
  };

  const handlePickerChange = (newRange) => {
    setDateRange(newRange);
    if (newRange.from && newRange.to) {
        setShowPicker(false);
    }
  };

  let datePickerButtonLabel = "Select Date Range";
  if (dateRange?.from) {
      datePickerButtonLabel = format(dateRange.from, "MMM d, yyyy");
      if (dateRange.to) {
          datePickerButtonLabel += ` - ${format(dateRange.to, "MMM d, yyyy")}`;
      }
  }

  const calendarPopover = (
      <Popover id="report-date-popover" className="custom-datepicker-popover">
          <Popover.Body>
              <CustomDatePicker range={dateRange} onRangeChange={handlePickerChange} />
          </Popover.Body>
      </Popover>
  );

  const currentData = useMemo(() => {
    if (activeTab === 'orders') return orderData?.orders || [];
    if (activeTab === 'top-selling') return topSellingData?.products || [];
    if (activeTab === 'low-stock') return lowStockData?.products || [];
    return [];
  }, [activeTab, orderData, topSellingData, lowStockData]);

  const currentColumns = useMemo(() => {
    if (activeTab === 'orders') return orderColumns;
    if (activeTab === 'top-selling') return topSellingColumns;
    if (activeTab === 'low-stock') return lowStockColumns;
    return [];
  }, [activeTab]);

  const lineChartData = useMemo(() => {
    if (activeTab === 'orders' && orderData?.orders) {
        const revenueByDate = orderData.orders.reduce((acc, order) => {
            const date = format(new Date(order.orderDate), 'yyyy-MM-dd');
            acc[date] = (acc[date] || 0) + order.totalAmount;
            return acc;
        }, {});
        return Object.entries(revenueByDate)
            .map(([date, revenue]) => ({ date, revenue }))
            .sort((a, b) => new Date(a.date) - new Date(b.date));
    }
    return [];
  }, [activeTab, orderData]);
  
  const groupedBarChartData = useMemo(() => {
    if (!orderData?.orders) {
        return [];
    }

    const relevantOrders = orderData.orders.filter(
        order => order.orderStatus === 'COMPLETED' || order.orderStatus === 'REFUNDED'
    );

    const dayDiff = dateRange.from && dateRange.to ? differenceInDays(dateRange.to, dateRange.from) : 0;
    
    let sortableDateFormat;
    let displayDateFormat;

    if (dayDiff <= 31) {
        sortableDateFormat = 'yyyy-MM-dd';
        displayDateFormat = 'MMM d';
    } else if (dayDiff <= 365) {
        sortableDateFormat = 'yyyy-MM';
        displayDateFormat = 'MMM yyyy';
    } else {
        sortableDateFormat = 'yyyy';
        displayDateFormat = 'yyyy';
    }

    const groupedData = relevantOrders.reduce((acc, order) => {
        const key = format(new Date(order.orderDate), sortableDateFormat);
        if (!acc[key]) {
            acc[key] = { 
                date: format(new Date(order.orderDate), displayDateFormat), 
                sortKey: key,
                COMPLETED: 0, 
                REFUNDED: 0 
            };
        }
        
        const status = order.orderStatus;
        if (status === 'COMPLETED' || status === 'REFUNDED') {
            acc[key][status] += order.totalAmount;
        }

        return acc;
    }, {});

    return Object.values(groupedData).sort((a, b) => a.sortKey.localeCompare(b.sortKey));
  }, [orderData, dateRange]);
  
  const statusBarDataKeys = [
    { key: 'COMPLETED', name: 'Completed', color: '#4ade80' },
    { key: 'REFUNDED', name: 'Refunded', color: '#f87171' },
  ];

  const isLoading = isOrdersLoading || isTopSellingLoading || isLowStockLoading;
  const isError = isOrdersError || isTopSellingError || isLowStockError;

  return (
    <>
      <MainHeader />
      <PageHeader
        title="DETAILED REPORTS"
        subtitle="Analyze sales, orders, and product performance"
        showBackButton={true}
        onBack={() => navigate(fromLocation)}
      />
      
      <div className="reports-page-container">
        <Tabs activeKey={activeTab} onSelect={handleTabSelect} id="report-tabs">
          <Tab eventKey="orders" title="Order Report"></Tab>
          <Tab eventKey="top-selling" title="Top-Selling Products"></Tab>
          <Tab eventKey="low-stock" title="Low Stock"></Tab>
        </Tabs>

        {activeTab === 'orders' && (
          <OrderFilters filters={filters} onFiltersChange={setFilters} logic={filterLogic} onLogicChange={setFilterLogic} />
        )}
        
        <ReportControlsHeader>
            {activeTab !== 'low-stock' ? (
                <OverlayTrigger
                    trigger="click"
                    placement="bottom-start"
                    show={showPicker}
                    onToggle={setShowPicker}
                    overlay={calendarPopover}
                    rootClose
                >
                    <Button variant="outline-secondary" className="date-range-picker-button">
                        <BsCalendarEvent className="me-2" />
                        {datePickerButtonLabel}
                    </Button>
                </OverlayTrigger>
            ) : <div />}
            
            <div className="d-flex align-items-center gap-3">
                <ButtonGroup>
                    <Button variant={viewMode === 'table' ? 'primary' : 'outline-secondary'} onClick={() => setViewMode('table')}>
                        <BsTable className="me-1"/> Table
                    </Button>
                    <Dropdown as={ButtonGroup}>
                        <Button variant={viewMode === 'chart' ? 'primary' : 'outline-secondary'} onClick={() => setViewMode('chart')}>
                        <BsBarChart className="me-1"/> Chart
                        </Button>
                        <Dropdown.Toggle split variant={viewMode === 'chart' ? 'primary' : 'outline-secondary'} id="dropdown-split-basic" />
                        <Dropdown.Menu>
                        {activeTab === 'orders' && (
                            <>
                            <Dropdown.Item active={chartType === 'revenueLine'} onClick={() => { setViewMode('chart'); setChartType('revenueLine'); }}>Revenue Line Chart</Dropdown.Item>
                            <Dropdown.Item active={chartType === 'statusBar'} onClick={() => { setViewMode('chart'); setChartType('statusBar'); }}>Status Bar Chart</Dropdown.Item>
                            </>
                        )}
                        {activeTab === 'top-selling' && (
                            <Dropdown.Item onClick={() => { setViewMode('chart'); }}>Quantity Sold Chart</Dropdown.Item>
                        )}
                        {activeTab === 'low-stock' && (
                            <Dropdown.Item onClick={() => { setViewMode('chart'); }}>Stock Remaining Chart</Dropdown.Item>
                        )}
                        </Dropdown.Menu>
                    </Dropdown>
                </ButtonGroup>
                <Button variant="outline-secondary" className="d-flex align-items-center gap-2" onClick={handleDownloadCsv} disabled={isLoading}><BsDownload/> Export CSV</Button>
            </div>
        </ReportControlsHeader>

        {isLoading && <div className="loading-overlay"><Spinner animation="border" /> Loading Report...</div>}
        {isError && <div className="error-overlay">An error occurred while fetching the report.</div>}
        
        {!isLoading && !isError && (
          <div className="report-content-wrapper">
            {viewMode === 'table' ? (
              <ReportTable data={currentData} columns={currentColumns} />
            ) : (
              <div className="chart-wrapper">
                {activeTab === 'orders' && chartType === 'revenueLine' && (
                  <ReportLineChart data={lineChartData} xAxisKey="date" dataKey="revenue" name="Revenue" color="#4ade80" isCurrency />
                )}
                {activeTab === 'orders' && chartType === 'statusBar' && (
                  <ReportGroupedBarChart data={groupedBarChartData} xAxisKey="date" dataKeys={statusBarDataKeys} isCurrency />
                )}
                {activeTab === 'top-selling' && <ReportBarChart data={currentData} yAxisKey="productName" dataKey="totalQuantitySold" name="Quantity Sold" color="#38bdf8" />}
                {activeTab === 'low-stock' && <ReportBarChart data={currentData} yAxisKey="componentName" dataKey="quantityRemaining" name="Stock Remaining" color="#facc15" />}
              </div>
            )}
          </div>
        )}
      </div>
    </>
  );
};

export default ReportsPage;