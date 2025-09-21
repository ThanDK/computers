import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FaTimesCircle, FaRedo, FaHeadset } from 'react-icons/fa';
import './StatusPage.css';

const PaymentFailedPage = () => {
    const [searchParams] = useSearchParams();
    const orderId = searchParams.get('order_id');

    return (
        <div className="status-page-container error">
            <div className="status-card">
                <FaTimesCircle className="status-icon" />
                <h1>การชำระเงินล้มเหลว</h1>
                <p>เกิดข้อผิดพลาดในการยืนยันการชำระเงิน กรุณาลองอีกครั้ง หรือติดต่อฝ่ายบริการลูกค้าหากปัญหายังคงอยู่</p>
                 <div className="order-id">
                    หมายเลขคำสั่งซื้อ: <strong>{orderId}</strong>
                </div>
                <div className="status-actions">
                    <Link to={`/orders/retry-payment/${orderId}`} className="status-button primary">
                        <FaRedo /> ลองชำระเงินอีกครั้ง
                    </Link>
                    <Link to="/contact-us" className="status-button secondary">
                        <FaHeadset /> ติดต่อเรา
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default PaymentFailedPage;