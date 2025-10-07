import React from 'react';

const contactInfo = {
    title: "Contact Us",
    description: "เราพร้อมให้ความช่วยเหลือและตอบทุกข้อสงสัยของคุณ สามารถติดต่อเราได้ตามช่องทางด้านล่าง",
    address: "123 IT SHOP Building, ถนนสุขุมวิท, กรุงเทพฯ 10110",
    phone: "02-123-4567",
    email: "support@itshop.com",
    openingHours: "จันทร์ - ศุกร์: 9:00 - 18:00 น."
};

const ContactUs = () => {
    return (
        <div className="container my-5">
            <div className="text-center mb-5">
                <img 
                    src="https://via.placeholder.com/1200x400?text=IT+Shop+Contact+Banner" 
                    className="img-fluid rounded shadow-sm" 
                    alt="Contact Us Banner" 
                />
            </div>

            <div className="text-center">
                <h1 className="display-4">{contactInfo.title}</h1>
                <p className="lead text-muted">{contactInfo.description}</p>
                <hr className="my-4" />

                <div className="row justify-content-center">
                    <div className="col-lg-8">
                        <div className="mb-4">
                            <h4 className="text-primary">ที่อยู่</h4>
                            <p className="fs-5">{contactInfo.address}</p>
                        </div>
                        <div className="mb-4">
                            <h4 className="text-primary">โทรศัพท์</h4>
                            <p className="fs-5">{contactInfo.phone}</p>
                        </div>
                        <div className="mb-4">
                            <h4 className="text-primary">อีเมล</h4>
                            <p className="fs-5">
                                <a href={`mailto:${contactInfo.email}`}>{contactInfo.email}</a>
                            </p>
                        </div>
                        <div className="mb-4">
                            <h4 className="text-primary">เวลาทำการ</h4>
                            <p className="fs-5">{contactInfo.openingHours}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ContactUs;