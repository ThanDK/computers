import React from 'react';

const howToOrderData = {
    title: "How to Order",
    steps: [
        {
            id: 1,
            title: "เลือกสินค้า",
            description: "เลือกชมสินค้าที่คุณต้องการในเว็บไซต์ของเรา และคลิกปุ่ม 'เพิ่มลงตะกร้า'"
        },
        {
            id: 2,
            title: "ตรวจสอบตะกร้าสินค้า",
            description: "ไปที่หน้าตะกร้าสินค้าเพื่อตรวจสอบรายการสินค้า, จำนวน, และราคารวม"
        },
        {
            id: 3,
            title: "ชำระเงิน",
            description: "กรอกข้อมูลที่อยู่สำหรับจัดส่ง และเลือกวิธีการชำระเงินที่คุณสะดวก"
        },
        {
            id: 4,
            title: "รอรับสินค้า",
            description: "หลังจากชำระเงินสำเร็จ เราจะดำเนินการจัดส่งสินค้าให้คุณโดยเร็วที่สุด"
        }
    ]
};

const HowToOrder = () => {
    return (
        <div className="container my-5">
            <h1 className="text-center mb-5">{howToOrderData.title}</h1>

            <div className="list-group">
                {howToOrderData.steps.map((step, index) => (
                    <div key={step.id} className="list-group-item list-group-item-action flex-column align-items-start">
                        <div className="d-flex w-100 justify-content-between">
                            <h5 className="mb-1">
                                <span className="badge bg-primary rounded-pill me-2">{index + 1}</span>
                                {step.title}
                            </h5>
                        </div>
                        <p className="mb-1 ms-4">{step.description}</p>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default HowToOrder;