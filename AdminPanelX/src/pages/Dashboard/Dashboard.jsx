import React, { useState, useEffect } from 'react';
import { Row, Col, Spinner, Table, Button, Modal, ButtonGroup } from 'react-bootstrap';
import { BsArrowUpRight, BsArrowDownRight, BsBoxSeam, BsGraphUp, BsHourglassSplit, BsFillBellFill, BsDownload, BsArchiveFill } from 'react-icons/bs';
import { subDays, subMonths, subYears, format, formatDistanceToNow } from 'date-fns';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import RevenueChart from '../../components/Charts/RevenueChart';
import TopSellingChart from '../../components/Charts/TopSellingChart';
import { fetchDashboardData, fetchOrdersForExport } from '../../services/DashboardService';
import { useAuth } from '../../context/AuthContext';
import './Dashboard.css';


const exportToCsv = (filename, rows) => {
    if (!rows || !rows.length) {
        return;
    }
    const separator = ',';
    const keys = Object.keys(rows[0]);
    const csvContent =
        keys.join(separator) +
        '\n' +
        rows.map(row => {
            return keys.map(k => {
                let cell = row[k] === null || row[k] === undefined ? '' : row[k];
                cell = cell instanceof Date 
                    ? cell.toLocaleString()
                    : cell.toString().replace(/"/g, '""');
                if (cell.search(/("|,|\n)/g) >= 0) {
                    cell = `"${cell}"`;
                }
                return cell;
            }).join(separator);
        }).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    if (link.download !== undefined) {
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
};


// --- Helper Components ---
const LoadingOverlay = () => <div className="d-flex justify-content-center align-items-center h-100"><Spinner animation="border" /></div>;
const NoDataMessage = ({ message }) => <div className="d-flex justify-content-center align-items-center h-100"><p className="text-secondary">{message}</p></div>;
const StatusBadge = ({ status }) => {
    const statusClass = status ? status.toLowerCase().replace(' ', '-') : 'default';
    return <span className={`status-badge status--${statusClass}`}>{status}</span>;
};

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

const DateRangeControls = ({ selectedRange, onRangeChange }) => {
    return (
        <ButtonGroup className="date-range-buttons">
            {rangeOptions.map(option => (
                <Button key={option.key} variant={selectedRange === option.key ? 'primary' : 'outline-secondary'} onClick={() => onRangeChange(option.key)}>
                    {option.label}
                </Button>
            ))}
        </ButtonGroup>
    );
};

function Dashboard() {
    const { token } = useAuth();
    const [loading, setLoading] = useState(true);
    const [isExporting, setIsExporting] = useState(false);
    const [data, setData] = useState(null);
    const [dateRange, setDateRange] = useState({ startDate: subDays(new Date(), 6), endDate: new Date() });
    const [selectedRange, setSelectedRange] = useState('7d');
    const [showLowStockModal, setShowLowStockModal] = useState(false);

    const handleRangeChange = (rangeKey) => {
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
        setDateRange({ startDate, endDate });
    };

    const handleExport = async () => {
        setIsExporting(true);
        try {
            const ordersToExport = await fetchOrdersForExport(token, dateRange.startDate, dateRange.endDate);
            
            if (ordersToExport && ordersToExport.length > 0) {
                const formattedStartDate = format(dateRange.startDate, 'yyyy-MM-dd');
                const formattedEndDate = format(dateRange.endDate, 'yyyy-MM-dd');
                const fileName = `orders-report-${formattedStartDate}-to-${formattedEndDate}.csv`;
                exportToCsv(fileName, ordersToExport);
            } else {
                alert("No order data available to export for the selected range.");
            }
        } catch (error) {
            console.error("Export failed:", error);
        } finally {
            setIsExporting(false);
        }
    };

    useEffect(() => {
        if (!token) { setLoading(false); return; }
        const loadDashboard = async () => {
            setLoading(true);
            try {
                const result = await fetchDashboardData(token, dateRange.startDate, dateRange.endDate);
                setData(result);
            } catch (error) {
                console.error("Failed to load dashboard data:", error);
                setData(null);
            } finally {
                setLoading(false);
            }
        };
        loadDashboard();
    }, [token, dateRange]);

    const formatCurrency = (val) => `฿${(val || 0).toLocaleString('en-US')}`;
    const formattedDateRange = `${format(dateRange.startDate, 'MMM d, yyyy')} - ${format(dateRange.endDate, 'MMM d, yyyy')}`;

    const getPeriodLabel = () => {
        const option = rangeOptions.find(opt => opt.key === selectedRange);
        if (option && option.key !== 'all') {
            return `LAST ${option.label.toUpperCase()}`;
        }
        return null; 
    };
    const periodLabel = getPeriodLabel();

    const statCards = loading || !data ? (
        Array.from({ length: 5 }).map((_, i) => (
            <Col key={i} xs={12} md={6} lg={4} className="mb-4"><div className="stat-card skeleton" style={{ height: '110px' }}/></Col>
        ))
    ) : (
        <>
            <Col xs={12} md={6} lg={4} className="mb-4"><StatCard title="Total Revenue" periodLabel={periodLabel} value={data.stats.totalRevenue} trend={data.stats.revenueChange} icon={<BsGraphUp />} isCurrency /></Col>
            <Col xs={12} md={6} lg={4} className="mb-4"><StatCard title="Total Sales" periodLabel={periodLabel} value={data.stats.totalSales} trend={data.stats.salesChange} icon={<BsBoxSeam />} /></Col>
            <Col xs={12} md={6} lg={4} className="mb-4"><StatCard title="Pending Orders" periodLabel={periodLabel} value={data.stats.pendingOrders} icon={<BsHourglassSplit />} /></Col>
            <Col xs={12} md={6} lg={4} className="mb-4"><StatCard title="Total Products" value={data.stats.products} icon={<BsArchiveFill />} /></Col>
            <Col xs={12} md={6} lg={4} className="mb-4"><StatCard title="Low Stock Alerts" value={data.stats.alerts} icon={<BsFillBellFill />} onClick={() => data.stats.alerts > 0 && setShowLowStockModal(true)} clickable={data.stats.alerts > 0} /></Col>
        </>
    );

    return (
        <>
            <MainHeader />
            <PageHeader title="DASHBOARD" subtitle={formattedDateRange} />

            <div className="dashboard-controls-container">
                <DateRangeControls selectedRange={selectedRange} onRangeChange={handleRangeChange} />
                <div>
                    <Button 
                        variant="primary" 
                        className="export-button" 
                        onClick={handleExport}
                        disabled={loading || isExporting}
                    >
                        {isExporting ? (
                            <>
                                <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true"/>
                                <span className="ms-2">Exporting...</span>
                            </>
                        ) : (
                            <><BsDownload /> Export Report</>
                        )}
                    </Button>
                </div>
            </div>

            <Row>{statCards}</Row>

            <Row>
                <Col xs={12} lg={7} className="mb-4">
                    <div className="chart-card chart-card--graph">
                        <div className="chart-header"><h4>Revenue Overview</h4></div>
                        <div className="chart-container">{loading ? <LoadingOverlay /> : (data?.revenueChartData?.length > 0 ? <RevenueChart data={data.revenueChartData} /> : <NoDataMessage message="No revenue data for this period." />)}</div>
                    </div>
                </Col>
                <Col xs={12} lg={5} className="mb-4">
                     <div className="chart-card chart-card--graph">
                        <div className="chart-header"><h4>Top Selling Products</h4></div>
                        <div className="chart-container">{loading ? <LoadingOverlay /> : (data?.topSellingData?.length > 0 ? <TopSellingChart data={data.topSellingData} /> : <NoDataMessage message="No sales data for this period." />)}</div>
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
                                            <tr>
                                                <th>Order ID</th>
                                                <th>Customer</th>
                                                <th>Status</th>
                                                <th>Amount</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {data?.recentOrders?.length > 0 ? data.recentOrders.map(order => (
                                                <tr key={order.id}>
                                                    <td>{order.id}</td>
                                                    <td>{order.customerName}</td>
                                                    <td><StatusBadge status={order.orderStatus} /></td>
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
                            <tbody>{data?.lowStockProducts?.map(product => (<tr key={product.id}><td>{product.name}</td><td>{product.mpn}</td><td className="text-end fw-bold text-warning">{product.stock}</td></tr>))}</tbody>
                        </Table>
                    </div>
                </Modal.Body>
            </Modal>
        </>
    );
}

export default Dashboard;