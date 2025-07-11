// src/pages/Dashboard/Dashboard.jsx

import React, { useState, useEffect } from 'react';
import { Row, Col, ButtonGroup, Button, Spinner } from 'react-bootstrap';
import { BsFillArchiveFill, BsHourglassSplit, BsCashCoin, BsFillBellFill } from 'react-icons/bs';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import RevenueChart from '../../components/Charts/RevenueChart';
import TopSellingChart from '../../components/Charts/TopSellingChart';
import { fetchDashboardData } from '../../services/DashboardService';
import { useAuth } from '../../context/AuthContext';
import './Dashboard.css';

const StatCard = ({ title, value, loading, icon, iconClass }) => (
    <div className="stat-card">
      <div className="stat-card__content">
        <div>
          <div className="stat-card__title">{title}</div>
          <div className="stat-card__value">{loading ? <Spinner animation="border" size="sm" /> : value}</div>
        </div>
        {React.cloneElement(icon, { className: `stat-card__icon ${iconClass}` })}
      </div>
    </div>
);

function Dashboard() {
  const [stats, setStats] = useState({ products: 0, pendingOrders: 0, totalRevenue: 0, alerts: 0 });
  const [revenueData, setRevenueData] = useState([]);
  const [topSellingData, setTopSellingData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState('monthly');
  const { token } = useAuth();

  useEffect(() => {
    const loadDashboard = async () => {
      if (!token) {
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        const data = await fetchDashboardData(token, timeRange);
        setStats(data.stats);
        setRevenueData(data.revenueChartData);
        setTopSellingData(data.topSellingChartData);
      } catch (error) {
        console.error("Failed to load dashboard data:", error);
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, [token, timeRange]);

  const revenueCardTitle = `REVENUE (${timeRange === 'monthly' ? 'LAST 30 DAYS' : 'THIS YEAR'})`;

  return (
    <>
      <MainHeader />
      <PageHeader title="DASHBOARD" subtitle="Welcome to your dashboard" />

      <Row>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <StatCard title="TOTAL PRODUCTS" value={stats.products} loading={loading} icon={<BsFillArchiveFill />} iconClass="icon-products"/>
        </Col>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <StatCard title="PENDING ORDERS" value={stats.pendingOrders} loading={loading} icon={<BsHourglassSplit />} iconClass="icon-pending"/>
        </Col>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <StatCard title={revenueCardTitle} value={`฿${stats.totalRevenue.toLocaleString('en-US', {maximumFractionDigits: 0})}`} loading={loading} icon={<BsCashCoin />} iconClass="icon-revenue"/>
        </Col>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <StatCard title="LOW STOCK ALERTS" value={stats.alerts} loading={loading} icon={<BsFillBellFill />} iconClass="icon-alerts"/>
        </Col>
      </Row>

      <Row>
        <Col xs={12} lg={8} className="mb-4">
          <div className="chart-card">
            <div className="chart-header">
              <h4>Revenue</h4>
              <ButtonGroup size="sm">
                <Button variant={timeRange === 'monthly' ? 'primary' : 'outline-secondary'} onClick={() => setTimeRange('monthly')}>Last 30 Days</Button>
                <Button variant={timeRange === 'yearly' ? 'primary' : 'outline-secondary'} onClick={() => setTimeRange('yearly')}>This Year</Button>
              </ButtonGroup>
            </div>
            <div className="chart-container">
                {loading ? (
                    <div className="chart-loading-overlay"><Spinner animation="border" /></div>
                ) : revenueData && revenueData.length > 0 ? (
                    <RevenueChart data={revenueData} />
                ) : (
                    <div className="chart-no-data-overlay"><p>No revenue data for this period.</p></div>
                )}
            </div>
          </div>
        </Col>
        <Col xs={12} lg={4} className="mb-4">
          <div className="chart-card">
            <div className="chart-header">
              <h4>Top 5 Selling Products</h4>
            </div>
             <div className="chart-container">
                {loading ? (
                    <div className="chart-loading-overlay"><Spinner animation="border" /></div>
                ) : topSellingData && topSellingData.length > 0 ? (
                    <TopSellingChart data={topSellingData} />
                ) : (
                    <div className="chart-no-data-overlay"><p>No sales data for this period.</p></div>
                )}
            </div>
          </div>
        </Col>
      </Row>
    </>
  );
}

export default Dashboard;