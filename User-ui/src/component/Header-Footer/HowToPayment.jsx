import React from 'react';

const paymentMethodsData = {
    title: "How to Payment",
    methods: [
        {
            id: "qrPayment",
            name: "ชำระผ่าน QR Code (PromptPay)",
            substeps: [
                { id: "qr1", detail: "เปิดแอปพลิเคชันธนาคารของคุณ" },
                { id: "qr2", detail: "เลือกเมนูสแกน QR Code" },
                { id: "qr3", detail: "สแกน QR Code ที่แสดงในหน้าชำระเงิน" },
                { id: "qr4", detail: "ยืนยันยอดชำระและทำรายการให้เสร็จสมบูรณ์" }
            ],
            notes: "ยอดชำระจะถูกอัปเดตโดยอัตโนมัติหลังจากทำรายการสำเร็จ",
            imageUrl: "https://via.placeholder.com/150/EEEEEE/000000?Text=QR+Code" 
        },
        {
            id: "paypal",
            name: "PayPal",
            substeps: [
                { id: "pp1", detail: "เลือก PayPal เป็นวิธีการชำระเงิน" },
                { id: "pp2", detail: "ระบบจะพาคุณไปยังหน้าเว็บไซต์ของ PayPal" },
                { id: "pp3", detail: "เข้าสู่ระบบบัญชี PayPal ของคุณ หรือสมัครหากยังไม่มี" },
                { id: "pp4", detail: "ตรวจสอบรายละเอียดการชำระเงินและยืนยัน" }
            ],
            notes: "โปรดตรวจสอบให้แน่ใจว่ายอดเงินในบัญชี PayPal เพียงพอต่อการชำระ",
            imageUrl: "https://via.placeholder.com/150/003087/FFFFFF?Text=PayPal"
        },
        {
            id: "bankTransfer",
            name: "โอนเงินผ่านธนาคาร",
            substeps: [
                { id: "bt1", detail: "ตรวจสอบข้อมูลบัญชีธนาคาร: กสิกรไทย, ชื่อบัญชี: บริษัท ไอที ช็อป จำกัด, เลขที่บัญชี: 123-4-56789-0" },
                { id: "bt2", detail: "ดำเนินการโอนเงินผ่านแอปพลิเคชันธนาคารหรือตู้ ATM" },
                { id: "bt3", detail: "แนบสลิปหรือหลักฐานการโอนเงินเพื่อยืนยันการชำระ" }
            ],
            notes: "การยืนยันการชำระอาจใช้เวลา 1-2 ชั่วโมงทำการ",
            imageUrl: null
        }
    ]
};

const HowToPayment = () => {
    return (
        <div className="container my-5">
            <h1 className="text-center mb-5">{paymentMethodsData.title}</h1>

            <div className="list-group">
                {paymentMethodsData.methods.map((method, methodIndex) => (
                    <div key={method.id} className="list-group-item list-group-item-action p-4">
                        <div className="row align-items-center g-3">
                            <div className="col-md-8">
                                <div className="d-flex align-items-center mb-3">
                                    <span className="badge bg-primary rounded-pill me-2">{methodIndex + 1}</span>
                                    <h4 className="mb-0">{method.name}</h4>
                                </div>
                                
                                <ul className="list-unstyled ms-4 mb-3">
                                    {method.substeps.map((step) => (
                                        <li key={step.id} className="d-flex align-items-center mb-2">
                                            <i className="bi bi-dot me-2 text-secondary fs-5"></i>
                                            <p className="mb-0">{step.detail}</p>
                                        </li>
                                    ))}
                                </ul>

                                {method.notes && (
                                    <p className="text-muted fst-italic ps-4">
                                        <i className="bi bi-info-circle me-2"></i>
                                        {method.notes}
                                    </p>
                                )}
                            </div>
                            
                            {method.imageUrl && (
                                <div className="col-md-4 text-center text-md-end">
                                    <img 
                                        src={method.imageUrl} 
                                        alt={method.name} 
                                        className="img-fluid rounded" 
                                        style={{ maxHeight: '120px' }}
                                    />
                                </div>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default HowToPayment;