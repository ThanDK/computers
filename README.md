# 🖥️ PC Builder & E-Commerce Platform

โปรเจค Full-Stack สำหรับระบบจำหน่ายชิ้นส่วนคอมพิวเตอร์ออนไลน์ ที่มาพร้อมฟังก์ชันจัดสเปคและตรวจสอบความเข้ากันได้ของฮาร์ดแวร์ (Hardware Compatibility Verification) ระบบถูกพัฒนาขึ้นโดยใช้ **Spring Boot (Java 21)** สำหรับ Backend API และ **React (Vite)** สำหรับส่วนติดต่อผู้ใช้ของผู้ดูแลระบบ (Admin Panel)

> **สถานะโปรเจค:** 🚧 กำลังอยู่ในระหว่างการพัฒนา โดยที่ส่วนของ User Panel ยังไม่เส็จขณะนี้จึงมีแค่ Admin Panel และ API (Work in Progress)

---

## ⚙️ ฟังก์ชันการทำงานหลัก (Core Functionality)

### Backend (Spring Boot Application)

1.  **🧠 กลไกตรวจสอบความเข้ากันได้ของฮาร์ดแวร์ (Smart Compatibility Engine)**
    - เป็นฟังก์ชันการทำงานหลักของโปรเจค ถูกพัฒนาขึ้นใน `ComponentCompatibilityServiceImpl` และ `CompatibilityHelperImpl` เพื่อวิเคราะห์ความสัมพันธ์ของชิ้นส่วนต่างๆ
    - **ตรรกะการตรวจสอบ (Validation Logic) ครอบคลุม:**
        - **Socket:** ความเข้ากันได้ระหว่าง CPU และ Motherboard
        - **RAM:** ประเภท (DDR4/5), จำนวนช่องติดตั้ง, และความจุสูงสุด
        - **Physical Dimensions:** ความยาวของการ์ดจอ (GPU) และความสูงของชุดระบายความร้อน (Cooler) เทียบกับพื้นที่ภายในเคส (Case)
        - **Form Factor:** ขนาดของ Motherboard (ATX/mATX) และ Power Supply (PSU) เทียบกับมาตรฐานที่เคสรองรับ
        - **Power Consumption:** คำนวณการใช้พลังงาน (Wattage) รวมของระบบ เพื่อเปรียบเทียบกับกำลังไฟของ PSU
        - **Storage Connectivity:** จำนวนพอร์ต M.2/SATA บน Motherboard เทียบกับจำนวนไดรฟ์ที่เลือก

2.  **💳 ระบบคำสั่งซื้อและการชำระเงิน (Order and Payment System)**
    - **รองรับหลายช่องทาง:**
        - **PayPal:** เชื่อมต่อกับ PayPal REST API สำหรับการสร้าง, ยืนยัน (Execute), และคืนเงิน (Refund)
        - **Bank Transfer:** มีกระบวนการสำหรับให้ผู้ใช้อัปโหลดหลักฐานการชำระเงิน (สลิป) ซึ่งไฟล์จะถูกจัดเก็บบน AWS S3 และมี API สำหรับให้ผู้ดูแลระบบอนุมัติหรือปฏิเสธ
    - **การจัดการสถานะ:** ใช้ Enums (`OrderStatus`, `PaymentStatus`) เพื่อควบคุมสถานะของคำสั่งซื้อในแต่ละขั้นตอนอย่างชัดเจน

3.  **📦 การจัดการสต็อกแบบ Atomic (Atomic Inventory Management)**
    - เพื่อป้องกันปัญหา Race Condition และรับประกันความถูกต้องของข้อมูล, ระบบใช้ `MongoTemplate` และ Bulk Write Operations (`bulkAtomicUpdateQuantities` ใน `InventoryRepositoryImpl`) สำหรับการปรับปรุงสต็อกสินค้าหลายรายการพร้อมกันในการดำเนินการเดียว

4.  **🔐 ระบบยืนยันตัวตนและจัดการสิทธิ์ (Authentication & Authorization)**
    - **Dual Authentication Methods:** รองรับการลงทะเบียนและล็อกอินด้วย Email/Password (ผ่าน JWT) และการล็อกอินผ่าน **Google (OAuth2)**
    - **Role-Based Access Control (RBAC):** ใช้ Spring Security ร่วมกับ Method Security (`@PreAuthorize`) เพื่อกำหนดสิทธิ์การเข้าถึง API สำหรับ `ROLE_ADMIN` และ `ROLE_USER`

5.  **🏛️ สถาปัตยกรรมเชิงวัตถุ (Object-Oriented Architecture)**
    - **Polymorphism:** ออกแบบ `Component` เป็น Abstract Class และมี Subclass สำหรับชิ้นส่วนแต่ละประเภท (Cpu, Gpu, etc.) เพื่อให้ง่ายต่อการเพิ่มประเภทสินค้าใหม่ในอนาคต
    - **Separation of Concerns:** มีการแบ่งแยกหน้าที่ความรับผิดชอบของแต่ละคลาสอย่างชัดเจนตามหลักการออกแบบซอฟต์แวร์ (Controller, Service, Repository, Mapper)

### Frontend (React Admin Panel)

1.  **📊 แดชบอร์ดสรุปข้อมูล (Data-Driven Dashboard)**
    - แสดงผลข้อมูลทางธุรกิจ เช่น ยอดขายรวม, จำนวนคำสั่งซื้อ, และสินค้าขายดี โดยใช้กราฟจากไลบรารี `Recharts`
    - มีฟังก์ชันสำหรับกรองข้อมูลตามช่วงเวลาและ Export ข้อมูลเป็นไฟล์ `.csv`

2.  **🎛️ ตารางจัดการข้อมูลแบบไดนามิก (URL-Driven Data Tables)**
    - พัฒนาโดยใช้ `Tanstack React Table` เพื่อสร้างตารางข้อมูลที่มีฟังก์ชันการค้นหา, การจัดเรียง (Sorting), และการแบ่งหน้า (Pagination)
    - **URL-Driven State:** สถานะของตาราง (เช่น หน้าปัจจุบัน, การจัดเรียง, ตัวกรอง) จะถูกบันทึกไว้ใน URL Query Parameters ทำให้สามารถแชร์ลิงก์หรือ Bookmark ได้

3.  **📝 ส่วนจัดการคำสั่งซื้อ (Order Management Interface)**
    - UI ในหน้า `OrderDetailPage` ถูกออกแบบมาให้สอดคล้องกับสถานะของคำสั่งซื้อจาก Backend
    - ผู้ดูแลระบบสามารถดำเนินการต่างๆ ได้ตามขั้นตอน เช่น การอนุมัติสลิป, การบันทึกข้อมูลการจัดส่ง, หรือการจัดการคืนเงิน

4.  **🖼️ ระบบจัดการและตัดรูปภาพ (Image Handling and Cropping)**
    - พัฒนา Component `ImageCropper` โดยใช้ `react-image-crop` และ HTML Canvas API เพื่อให้ผู้ใช้สามารถตัดรูปภาพ (Crop) ได้ก่อนการอัปโหลด

5.  **🏗️ สถาปัตยกรรมแบบ Component-Based (Reusable Component Architecture)**
    - สร้าง UI Components ที่สามารถนำกลับมาใช้ใหม่ได้ทั่วทั้งโปรเจค เช่น `ReusableTable`, `ConfirmationModal`, `PageHeader`, `StatusBadge`
    - **Dynamic Form Generation:** ใช้ Object Configuration (`COMPONENT_CONFIG`) ในการสร้างฟอร์มสำหรับชิ้นส่วนคอมพิวเตอร์แต่ละประเภทโดยอัตโนมัติ เพื่อลดการเขียนโค้ดซ้ำซ้อน

---

## 🛠️ เทคโนโลยีที่ใช้ (Tech Stack)

| ส่วน | เทคโนโลยี | รายละเอียดและการใช้งาน |
|---|---|---|
| **Backend** | `Java 21`, `Spring Boot 3`, `Maven` | พัฒนา RESTful API และ Business Logic หลักของระบบ |
| **Frontend** | `React (Vite)`, `React Router` | ส่วนติดต่อผู้ใช้สำหรับผู้ดูแลระบบในรูปแบบ Single Page Application (SPA) |
| **Database** | `MongoDB` | ฐานข้อมูล NoSQL สำหรับจัดเก็บข้อมูลสินค้า, ผู้ใช้, และคำสั่งซื้อ |
| **Styling** | `React-Bootstrap`, `CSS Modules` | ออกแบบและจัดวางส่วนประกอบ UI ให้รองรับขนาดหน้าจอที่แตกต่างกัน |
| **State Mgt.** | `React Context API` | จัดการสถานะการล็อกอินและข้อมูลผู้ใช้ส่วนกลางใน Frontend |
| **Data Tables** | `Tanstack React Table` | สร้างตารางข้อมูลที่มีฟังก์ชันการทำงานขั้นสูง |
| **Data Viz** | `Recharts` | แสดงผลข้อมูลในรูปแบบกราฟบนหน้า Dashboard |
| **Authentication** | `Spring Security`, `JWT`, `Google OAuth2` | จัดการการยืนยันตัวตนและกำหนดสิทธิ์การเข้าถึง (RBAC) |
| **Cloud Storage** | `Amazon Web Services (AWS S3)` | จัดเก็บไฟล์รูปภาพและเอกสารต่างๆ |
| **Payment** | `PayPal REST API` | เชื่อมต่อระบบการชำระเงินออนไลน์ |
| **Deployment** | `Docker`, `Docker Compose` | สร้าง Container และจัดการการทำงานของ Services ทั้งหมด |

---

## 🚀 การติดตั้งและเริ่มต้นใช้งาน (Getting Started)

### สิ่งที่ต้องมี (Prerequisites)
- Java JDK 21+
- Apache Maven
- Node.js & npm
- Docker & Docker Compose
- API Keys จาก AWS, PayPal Developer, และ Google Cloud Platform

### 1. ตั้งค่า Environment Variables
สร้างไฟล์ `.env` ที่ root ของโปรเจค (ระดับเดียวกับ `docker-compose.yml`) และกำหนดค่าที่จำเป็น:

```env
# MongoDB Connection
SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/computers

# JWT Secret Key
JWT_SECRET_KEY=YourGeneratedStrongAndRandomSecretKey

# AWS S3 Bucket Credentials
AWS_ACCESS_KEY=YOUR_AWS_ACCESS_KEY
AWS_SECRET_KEY=YOUR_AWS_SECRET_KEY

# PayPal Developer Credentials
PAYPAL_CLIENT_ID=YOUR_PAYPAL_SANDBOX_CLIENT_ID
PAYPAL_CLIENT_SECRET=YOUR_PAYPAL_SANDBOX_SECRET

# Google OAuth2 Credentials
GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
```

### 2. การรันโปรเจคด้วย Docker (วิธีที่แนะนำ)
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/ThanDK/OnlineShopComputerBuilder.git
    cd OnlineShopComputerBuilder
    ```
2.  **Start all services:**
    ```bash
    docker-compose up --build
    ```
    - **Backend API** จะทำงานที่ `http://localhost:8080`
    - **Admin Panel** จะทำงานที่ `http://localhost:5173`
    - **MongoDB** จะทำงานภายใน Docker network

---

## 📁 โครงสร้างโปรเจค (Project Structure)

```
/
├── AdminPanelX/          # Frontend (React) สำหรับ Admin
│   ├── public/
│   └── src/
│       ├── components/   # (Reusable UI Components)
│       ├── context/      # (AuthContext for global state)
│       ├── layouts/      # (AdminLayout)
│       ├── pages/        # (Each page of the application)
│       └── services/     # (API call functions)
├── computers/            # Backend (Spring Boot)
│   └── src/main/java/in/project/computers/
│       ├── config/       # (SecurityConfig, AWSConfig, etc.)
│       ├── controller/   # (API Endpoints)
│       ├── DTO/          # (Data Transfer Objects)
│       ├── entity/       # (MongoDB Document Models)
│       └── service/      # (Business Logic, Compatibility Engine)
├── .env                  # Environment variables for Docker Compose
├── docker-compose.yml    # Docker Compose configuration
└── pom.xml               # Backend Maven dependencies
```

---

## 💡 สิ่งที่สามารถพัฒนาต่อได้ (Future Improvements)

-   **[ ] User Panel (E-Commerce Frontend):** พัฒนาส่วนหน้าสำหรับลูกค้าทั่วไปเพื่อเลือกซื้อสินค้าและจัดสเปค
-   **[ ] Unit & Integration Tests:** เพิ่มการทดสอบในส่วนต่างๆ ของ Backend เพื่อเพิ่มความเสถียรของระบบ
-   **[ ] CI/CD Pipeline:** สร้าง Pipeline สำหรับการ Build และ Deploy อัตโนมัติเมื่อมีการ Push code
-   **[ ] Elasticsearch Integration:** เพิ่มประสิทธิภาพการค้นหาสินค้าด้วย Elasticsearch
-   **[ ] ระบบแจ้งเตือน (Notification System):** แจ้งเตือนผู้ดูแลระบบเมื่อมีออเดอร์ใหม่หรือมีเหตุการณ์สำคัญ
