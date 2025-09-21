import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FaCheckCircle, FaClipboardList, FaHome } from 'react-icons/fa';
import './StatusPage.css';

const PaymentSuccessfulPage = () => {
    const [searchParams] = useSearchParams();
    const orderId = searchParams.get('order_id');

    return (
        <div className="status-page-container success">
            <div className="status-card">
                <FaCheckCircle className="status-icon" />
                <h1>การชำระเงินสำเร็จ!</h1>
                <p>คำสั่งซื้อของคุณได้รับการยืนยันแล้ว เราได้ส่งอีเมลสรุปรายการให้คุณเรียบร้อย</p>
                <div className="order-id">
                    หมายเลขคำสั่งซื้อ: <strong>{orderId}</strong>
                </div>
                <div className="status-actions">
                    <Link to="/profile/orders" className="d-flex justify-content-between align-items-center">
                        <FaClipboardList /> ดูรายละเอียดคำสั่งซื้อ
                    </Link>
                    <Link to="/" className="status-button secondary">
                        <FaHome /> กลับสู่หน้าหลัก
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default PaymentSuccessfulPage;