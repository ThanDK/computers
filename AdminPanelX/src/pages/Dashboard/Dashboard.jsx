import React, { useState } from 'react';
import { Row, Col, Spinner, Table, Button, Modal, ButtonGroup, Popover, OverlayTrigger } from 'react-bootstrap';
import {
    BsArrowUpRight, BsArrowDownRight, BsBoxSeam, BsGraphUp,
    BsHourglassSplit, BsFillBellFill, BsClipboardData, BsArchiveFill, BsCalendarEvent
} from 'react-icons/bs';
import { subDays, subMonths, subYears, format } from 'date-fns';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useLocation } from 'react-router-dom'; // MODIFIED: Imported useLocation
import { CustomDatePicker } from '../../components/DatePicker/CustomDatePicker';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import RevenueChart from '../../components/Charts/RevenueChart';
import TopSellingChart from '../../components/Charts/TopSellingChart';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import { fetchDashboardData } from '../../services/DashboardService';
import { useAuth } from '../../context/AuthContext';

import './Dashboard.css';

const LoadingOverlay = () => <div className="d-flex justify-content-center align-items-center h-100"><Spinner animation="border" /></div>;
const NoDataMessage = ({ message }) => <div className="d-flex justify-content-center align-items-center h-100"><p className="text-secondary">{message}</p></div>;

const StatCard = ({ title, value, trend, icon, periodLabel, isCurrency = false, onClick, clickable = false }) => {
    const isPositive = trend >= 0;
    const showTrend = typeof trend === 'number' && !isNaN(trend);
    const displayTitle = periodLabel ? `${title.toUpperCase()} (${periodLabel})` : title.toUpperCase();

    return (
        <div className={`stat-card ${clickable ? 'stat-card--clickable' : ''}`} onClick={onClick}>
            <div className="stat-card__icon">{icon}</div>
            <div className="stat-card__details">
                <div className="stat-card__title">{displayTitle}</div>
                <div className="stat-card__value">{isCurrency ? `฿${(value || 0).toLocaleString('en-US')}` : (value || 0).toLocaleString()}</div>
                {showTrend && (
                    <div className={`stat-card__trend ${isPositive ? 'positive' : 'negative'}`}>
                        {isPositive ? <BsArrowUpRight /> : <BsArrowDownRight />}
                        {Math.abs(trend).toFixed(1)}% vs. previous period
                    </div>
                )}
            </div>
        </div>
    );
};

const rangeOptions = [
    { key: '7d', label: '7 Days' }, { key: '15d', label: '15 Days' }, { key: '30d', label: '30 Days' },
    { key: '3m', label: '3 Months' }, { key: '1y', label: '1 Year' }, { key: 'all', label: 'All Time' },
];

const DateRangeControls = ({ selectedRange, onRangeChange }) => (
    <ButtonGroup className="date-range-buttons">
        {rangeOptions.map(option => (
            <Button
                key={option.key}
                variant={selectedRange === option.key ? 'primary' : 'outline-secondary'}
                onClick={() => onRangeChange(option.key)}
            >
                {option.label}
            </Button>
        ))}
    </ButtonGroup>
);

function Dashboard() {
    const { token } = useAuth();
    const navigate = useNavigate();
    const location = useLocation(); // MODIFIED: Initialized useLocation
    const [range, setRange] = useState({ from: subDays(new Date(), 6), to: new Date() });
    const [selectedRange, setSelectedRange] = useState('7d');
    const [showLowStockModal, setShowLowStockModal] = useState(false);
    const [showPopover, setShowPopover] = useState(false);

    const { data, isLoading, isFetching } = useQuery({
        queryKey: ['dashboardData', range],
        queryFn: () => fetchDashboardData(token, range.from, range.to),
        enabled: !!token && !!range.from && !!range.to,
        placeholderData: (previousData) => previousData,
    });

    const handleStaticRangeChange = (rangeKey) => {
        const endDate = new Date();
        let startDate;

        switch (rangeKey) {
            case '7d': startDate = subDays(endDate, 6); break;
            case '15d': startDate = subDays(endDate, 14); break;
            case '30d': startDate = subDays(endDate, 29); break;
            case '3m': startDate = subMonths(endDate, 3); break;
            case '1y': startDate = subYears(endDate, 1); break;
            case 'all': startDate = new Date('2020-01-01'); break;
            default: startDate = subDays(endDate, 6);
        }

        setSelectedRange(rangeKey);
        setRange({ from: startDate, to: endDate });
    };

    const handleCustomRangeChange = (newRange) => {
        setRange(newRange);
        if (newRange.from && newRange.to) {
            setSelectedRange('custom');
            setShowPopover(false);
        }
    }

    const handleViewReports = () => {
        // MODIFIED: Pass current location in state
        navigate('/reports', { state: { from: location } });
    };

    const formatCurrency = (val) => `฿${(val || 0).toLocaleString('en-US')}`;
    
    let formattedDateRange = "Loading...";
    if (range?.from && range?.to) {
        formattedDateRange = `${format(range.from, 'MMM d, yyyy')} - ${format(range.to, 'MMM d, yyyy')}`;
    }

    let datePickerButtonLabel = "Select Date Range";
    if (range?.from) {
        datePickerButtonLabel = format(range.from, "MMM d, yyyy");
        if (range.to) {
            datePickerButtonLabel += ` - ${format(range.to, "MMM d, yyyy")}`;
        }
    }

    const getPeriodLabel = () => {
        const option = rangeOptions.find(opt => opt.key === selectedRange);
        return (option && option.key !== 'all') ? `LAST ${option.label.toUpperCase()}` : null;
    };

    const periodLabel = getPeriodLabel();
    const loading = isLoading || isFetching;

    const statCards = isLoading && !data ? (
        Array.from({ length: 5 }).map((_, i) => (
            <Col key={i} xs={12} md={6} lg={4} className="mb-4">
                <div className="stat-card skeleton" style={{ height: '110px' }}/>
            </Col>
        ))
    ) : (
        <>
            <Col xs={12} md={6} lg={4} className="mb-4">
                <StatCard title="Total Revenue" periodLabel={periodLabel} value={data?.stats.totalRevenue} trend={data?.stats.revenueChange} icon={<BsGraphUp />} isCurrency />
            </Col>
            <Col xs={12} md={6} lg={4} className="mb-4">
                <StatCard title="Total Sales" periodLabel={periodLabel} value={data?.stats.totalSales} trend={data?.stats.salesChange} icon={<BsBoxSeam />} />
            </Col>
            <Col xs={12} md={6} lg={4} className="mb-4">
                <StatCard title="Pending Orders" periodLabel={periodLabel} value={data?.stats.pendingOrders} icon={<BsHourglassSplit />} />
            </Col>
            <Col xs={12} md={6} lg={4} className="mb-4">
                <StatCard title="Total Products" value={data?.stats.products} icon={<BsArchiveFill />} />
            </Col>
            <Col xs={12} md={6} lg={4} className="mb-4">
                <StatCard title="Low Stock Alerts" value={data?.stats.alerts} icon={<BsFillBellFill />} onClick={() => data?.stats.alerts > 0 && setShowLowStockModal(true)} clickable={data?.stats.alerts > 0} />
            </Col>
        </>
    );
    
    const calendarPopover = (
        <Popover id="date-range-popover" className="custom-datepicker-popover">
            <Popover.Body>
                <CustomDatePicker range={range} onRangeChange={handleCustomRangeChange} />
            </Popover.Body>
        </Popover>
    );

    return (
        <>
            <MainHeader />
            <PageHeader title="DASHBOARD" subtitle={formattedDateRange} />

            <div className="dashboard-controls-container">
                <div className="d-flex align-items-center gap-2">
                    <DateRangeControls selectedRange={selectedRange} onRangeChange={handleStaticRangeChange} />
                    <OverlayTrigger
                        trigger="click"
                        placement="bottom-start"
                        show={showPopover}
                        onToggle={setShowPopover}
                        overlay={calendarPopover}
                        rootClose
                    >
                        <Button variant="outline-secondary" className="date-range-picker-button">
                            <BsCalendarEvent className="me-2" />
                            {datePickerButtonLabel}
                        </Button>
                    </OverlayTrigger>
                </div>

                <div>
                    <Button variant="primary" className="export-button" onClick={handleViewReports}>
                        <BsClipboardData /> View Detailed Reports
                    </Button>
                </div>
            </div>

            <Row>{statCards}</Row>

            <Row>
                <Col xs={12} lg={7} className="mb-4">
                    <div className="chart-card chart-card--graph">
                        <div className="chart-header"><h4>Revenue Overview</h4></div>
                        <div className="chart-container">
                            {loading ? <LoadingOverlay /> : (
                                data?.revenueChartData?.length > 0
                                ? <RevenueChart data={data.revenueChartData} />
                                : <NoDataMessage message="No revenue data for this period." />
                            )}
                        </div>
                    </div>
                </Col>
                <Col xs={12} lg={5} className="mb-4">
                     <div className="chart-card chart-card--graph">
                        <div className="chart-header"><h4>Top Selling Products</h4></div>
                        <div className="chart-container">
                            {loading ? <LoadingOverlay /> : (
                                data?.topSellingData?.length > 0
                                ? <TopSellingChart data={data.topSellingData} />
                                : <NoDataMessage message="No sales data for this period." />
                            )}
                        </div>
                    </div>
                </Col>
            </Row>

            <Row>
                 <Col xs={12} className="mb-4">
                    <div className="chart-card">
                        <div className="chart-header"><h4>Recent Orders</h4></div>
                        <div className="chart-container">
                            <div className="recent-orders-table-container">
                                {loading ? <LoadingOverlay /> : (
                                    <Table hover responsive className="recent-orders-table">
                                        <thead>
                                            <tr><th>Order ID</th><th>Customer</th><th>Status</th><th>Amount</th></tr>
                                        </thead>
                                        <tbody>
                                            {data?.recentOrders?.length > 0 ? data.recentOrders.map(order => (
                                                <tr key={order.id}>
                                                    <td>{order.id}</td>
                                                    <td>{order.customerName}</td>
                                                    <td><StatusBadge status={order.orderStatus} type="order" /></td>
                                                    <td>{formatCurrency(order.totalAmount)}</td>
                                                </tr>
                                            )) : (
                                                <tr><td colSpan="4" className="text-center text-secondary py-5">No recent orders.</td></tr>
                                            )}
                                        </tbody>
                                    </Table>
                                )}
                            </div>
                        </div>
                    </div>
                </Col>
            </Row>

            <Modal show={showLowStockModal} onHide={() => setShowLowStockModal(false)} size="lg" centered>
                <Modal.Header closeButton closeVariant="white"><Modal.Title>Low Stock Products</Modal.Title></Modal.Header>
                <Modal.Body>
                    <div className="modal-table-container">
                        <Table hover responsive className="recent-orders-table">
                            <thead><tr><th>Product Name</th><th>MPN</th><th className="text-end">Stock Remaining</th></tr></thead>
                            <tbody>
                                {data?.lowStockProducts?.map(product => (
                                    <tr key={product.id}>
                                        <td>{product.name}</td>
                                        <td>{product.mpn}</td>
                                        <td className="text-end fw-bold text-warning">{product.stock}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </Table>
                    </div>
                </Modal.Body>
            </Modal> 
        </>
    );
}

export default Dashboard;