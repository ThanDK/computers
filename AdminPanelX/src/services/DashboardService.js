import { format } from 'date-fns';

const API_BASE_URL = 'http://localhost:8080/api';

/**
 * ดึงข้อมูลทั้งหมดสำหรับแสดงผลบนหน้า Dashboard หลัก
 * @param {string} token - JWT token
 * @param {Date} startDate - วันที่เริ่มต้นของช่วงข้อมูล
 * @param {Date} endDate - วันที่สิ้นสุดของช่วงข้อมูล
 * @returns {Promise<object>} Promise ที่จะ resolve เป็น object ที่มีข้อมูล dashboard ทั้งหมด
 */
export async function fetchDashboardData(token, startDate, endDate) {
    
    const startQuery = format(startDate, 'yyyy-MM-dd');
    const endQuery = format(endDate, 'yyyy-MM-dd');
    
    const url = `${API_BASE_URL}/admin/dashboard?startDate=${startQuery}&endDate=${endQuery}`;

    const response = await fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Failed to fetch dashboard data.');
    }

    const data = await response.json();

    // จัดรูปแบบข้อมูลที่ได้รับจาก API ให้พร้อมใช้งานใน frontend
    return {
        stats: data.stats,
        revenueChartData: data.revenueChartData.map(item => ({
            date: new Date(item.name + 'T00:00:00Z').toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
            revenue: item.value
        })),
        topSellingData: data.topSellingData.map(item => ({
            name: item.name,
            quantitySold: item.value 
        })),
        recentOrders: data.recentOrders,
        lowStockProducts: data.lowStockProducts || [],
    };
};

/**
 * ดึงข้อมูล "เฉพาะ" ออเดอร์สำหรับ Export ตามช่วงวันที่ที่กำหนด
 * @param {string} token - JWT token
 * @param {Date} startDate - วันที่เริ่มต้น
 * @param {Date} endDate - วันที่สิ้นสุด
 * @returns {Promise<Array>} Promise ที่จะ resolve เป็น Array ของข้อมูลออเดอร์สำหรับ export
 */
export async function fetchOrdersForExport(token, startDate, endDate) {
    const startQuery = format(startDate, 'yyyy-MM-dd');
    const endQuery = format(endDate, 'yyyy-MM-dd');
    
    const url = `${API_BASE_URL}/admin/dashboard/export?startDate=${startQuery}&endDate=${endQuery}`;

    const response = await fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Failed to fetch order data for export.');
    }
        
    return response.json(); 
};