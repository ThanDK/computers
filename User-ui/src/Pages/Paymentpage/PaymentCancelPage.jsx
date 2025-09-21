import React from 'react';
import { Link } from 'react-router-dom';
import { Card, Button } from 'react-bootstrap';
import { XCircleFill } from 'react-bootstrap-icons';

const PaymentCancelPage = () => {
  return (
    <div className="container mt-5">
      <Card className="text-center border-warning">
        <Card.Body>
          <XCircleFill size={50} className="text-warning mb-3" />
          <Card.Title as="h2">การชำระเงินถูกยกเลิก</Card.Title>
          <Card.Text>
            คุณได้ยกเลิกการชำระเงินสำหรับคำสั่งซื้อ. คำสั่งซื้อของคุณยังไม่เสร็จสมบูรณ์ คุณสามารถกลับไปชำระเงินได้อีกครั้งจากหน้ารายละเอียดคำสั่งซื้อ.
          </Card.Text>
          <div className="d-flex justify-content-center gap-2 mt-4">
            <Button as={Link} to="/profile/orders" className="d-flex justify-content-between align-items-center">
              Go to Your Order
            </Button>
            <Button as={Link} to="/" variant="outline-secondary">
              Go to Homepage
            </Button>
          </div>
        </Card.Body>
      </Card>
    </div>
  );
};

export default PaymentCancelPage;