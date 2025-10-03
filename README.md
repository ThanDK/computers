# 🖥️ PC Builder & Technical Showcase Platform

โปรเจค Full-Stack นี้ถูกสร้างขึ้นเพื่อเป็น **"Technical Showcase"** สำหรับการสาธิตและฝึกฝนทักษะการพัฒนาซอฟต์แวร์ขั้นสูง โดยจำลองระบบจัดสเปคคอมพิวเตอร์ออนไลน์ที่มาพร้อมฟังก์ชันตรวจสอบความเข้ากันได้ของฮาร์ดแวร์แบบ Real-time ระบบถูกพัฒนาขึ้นโดยใช้ **Spring Boot (Java 21)** สำหรับ Backend API และ **React (Vite)** สำหรับส่วนติดต่อผู้ใช้สองส่วนคือ ส่วนของผู้ดูแลระบบ (Admin Panel) และส่วนของผู้ใช้งานทั่วไป (User Panel)

> **หมายเหตุ:** โปรเจคนี้ไม่ใช่แพลตฟอร์ม E-commerce สำหรับการใช้งานจริง แต่เป็น **"Training Project"** ที่มุ่งเน้นการแก้ปัญหาทางเทคนิคที่ซับซ้อนและแสดงให้เห็นถึงความเข้าใจในสถาปัตยกรรมซอฟต์แวร์ที่ทันสมัย โดยให้ความสำคัญกับความสมบูรณ์ของฟังก์ชันและคุณภาพของโค้ด

---

> **สถานะปัจจุบันของโปรเจค**
>
> ✅ **Core Features Completed**
>
> โปรเจคนี้ได้พัฒนาฟังก์ชันหลักที่จำเป็นสำหรับแพลตฟอร์มสาธิตทักษะทางเทคนิคเสร็จสมบูรณ์แล้ว ทั้งในส่วนของ Backend API, ส่วนจัดการสำหรับผู้ดูแลระบบ (Admin Panel), และส่วนติดต่อสำหรับผู้ใช้ทั่วไป (User Panel) พร้อมสำหรับการสาธิตการใช้งาน
>
> **สรุปสถานะของแต่ละส่วน:**
>
> ---
>
> ### ✅ **1. Backend API (Spring Boot)** - **เสถียรและพร้อมใช้งาน**
>
> API หลักของระบบได้ถูกพัฒนาจนมีฟังก์ชันการทำงานที่ครอบคลุมสำหรับการจัดการทั้งหมดแล้ว ซึ่งประกอบด้วย:
> - **Authentication & Authorization:** ระบบล็อกอินด้วย JWT และ Google OAuth2 พร้อมการจำกัดสิทธิ์แบบ Role-based ทำงานได้อย่างสมบูรณ์
> - **Smart Compatibility Engine:** กลไกการตรวจสอบความเข้ากันได้ของฮาร์ดแวร์ถูกพัฒนาและทดสอบแล้ว
> - **Inventory Management:** ระบบ CRUD สำหรับชิ้นส่วนคอมพิวเตอร์และระบบจัดการสต็อกแบบ Atomic ทำงานได้อย่างถูกต้อง
> - **Order Management:** กระบวนการสั่งซื้อ, การชำระเงินผ่าน PayPal, และการโอนเงิน (พร้อมอัปโหลดสลิป) ถูกพัฒนาเสร็จสิ้น
> - **Admin Endpoints:** API สำหรับผู้ดูแลระบบในการจัดการข้อมูลทั้งหมด (Users, Orders, Components, Lookups) พร้อมใช้งาน
>
> ---
>
> ### ✅ **2. Admin Panel (React)** - **ฟังก์ชันครบถ้วนและพร้อมสาธิต**
>
> ส่วนติดต่อผู้ใช้สำหรับผู้ดูแลระบบสามารถใช้งานฟังก์ชันต่างๆ ที่เชื่อมต่อกับ Backend API ได้อย่างครบถ้วน:
> - **Dashboard:** แสดงผลข้อมูลสรุป, กรองตามช่วงเวลา, และ Export Report (หมายเหตุ: ฟังก์ชัน Export ยังอยู่ในระหว่างการพัฒนาและอาจทำงานได้ไม่สมบูรณ์)
> - **Component Management:** สามารถ เพิ่ม, ลบ, แก้ไข, ค้นหา, และจัดการสต็อกสินค้าได้
> - **Order Management:** สามารถดูรายการคำสั่งซื้อ, ตรวจสอบรายละเอียด, อนุมัติ/ปฏิเสธสลิป, บันทึกข้อมูลการจัดส่ง, และจัดการการคืนเงินได้
> - **User Management:** สามารถจัดการข้อมูลและสถานะของผู้ใช้ในระบบได้ (เช่น Lock/Unlock account)
> - **Lookup Management:** สามารถจัดการข้อมูลพื้นฐานที่ใช้ในฟอร์มต่างๆ ได้ (เช่น Sockets, RAM Types, Brands)
>
> ---
>
> ### ✅ **3. User Panel (E-Commerce Frontend)** - **ฟังก์ชันครบถ้วนและพร้อมใช้งาน**
>
> ส่วนติดต่อผู้ใช้สำหรับลูกค้าทั่วไปได้พัฒนาเสร็จสมบูรณ์แล้ว โดยมีฟีเจอร์หลักดังนี้:
> - **Product Browsing:** หน้าสำหรับเลือกดูและค้นหาสินค้า พร้อมระบบกรองและจัดเรียง
> - **PC Builder UI:** ส่วนติดต่อผู้ใช้สำหรับการจัดสเปคคอมพิวเตอร์ ซึ่งจะเรียกใช้ Compatibility Engine แบบ Real-time เพื่อแสดงคำแนะนำและข้อผิดพลาด
> - **Shopping Cart:** ระบบตะกร้าสินค้าสำหรับจัดการรายการที่เลือก
> - **Checkout Process:** กระบวนการสั่งซื้อและเลือกวิธีการชำระเงินที่เชื่อมต่อกับ Backend
> - **User Profile & Order History:** หน้าสำหรับให้ผู้ใช้ดูข้อมูลส่วนตัวและประวัติการสั่งซื้อของตนเอง

---

## ⚙️ ฟังก์ชันการทำงานหลัก

### Backend (Spring Boot Application)

1.  **🧠 กลไกตรวจสอบความเข้ากันได้ของฮาร์ดแวร์**
    - เป็นฟังก์ชันการทำงานหลักของโปรเจค ถูกพัฒนาขึ้นใน `ComponentCompatibilityServiceImpl` และ `CompatibilityHelperImpl` เพื่อวิเคราะห์ความสัมพันธ์ของชิ้นส่วนต่างๆ
    - **ตรรกะการตรวจสอบครอบคลุม:**
        - **Socket:** ความเข้ากันได้ระหว่าง CPU และ Motherboard
        - **RAM:** ประเภท, จำนวนช่องติดตั้ง, และความจุสูงสุด
        - **Physical Dimensions:** ความยาวของการ์ดจอ และความสูงของชุดระบายความร้อน เทียบกับพื้นที่ภายในเคส
        - **Form Factor:** ขนาดของ Motherboard และ Power Supply เทียบกับมาตรฐานที่เคสรองรับ
        - **Power Consumption:** คำนวณ Wattage รวมของระบบ เพื่อเปรียบเทียบกับกำลังไฟของ PSU
        - **Storage Connectivity:** จำนวนพอร์ต M.2/SATA บน Motherboard เทียบกับจำนวนไดรฟ์ที่เลือก

2.  **💳 ระบบจำลองคำสั่งซื้อและการชำระเงิน**
    - **รองรับหลายช่องทาง:**
        - **PayPal:** เชื่อมต่อกับ PayPal REST API สำหรับการสร้าง, ยืนยัน, และคืนเงิน
        - **Bank Transfer:** มีกระบวนการสำหรับให้ผู้ใช้อัปโหลดสลิป ซึ่งไฟล์จะถูกจัดเก็บบน AWS S3 และมี API สำหรับให้ผู้ดูแลระบบอนุมัติหรือปฏิเสธ
    - **การจัดการสถานะ:** ใช้ Enums เช่น `OrderStatus` และ `PaymentStatus` เพื่อควบคุมสถานะของคำสั่งซื้อในแต่ละขั้นตอนอย่างชัดเจน

3.  **📦 การจัดการสต็อกแบบ Atomic**
    - เพื่อป้องกันปัญหา Race Condition ระบบใช้ `MongoTemplate` และ Bulk Write Operations ผ่านเมธอด `bulkAtomicUpdateQuantities` สำหรับการปรับปรุงสต็อกสินค้าหลายรายการพร้อมกันในการดำเนินการเดียว

4.  **🔐 ระบบยืนยันตัวตนและจัดการสิทธิ์**
    - **Dual Authentication Methods:** รองรับการล็อกอินด้วย Email/Password (ผ่าน JWT) และการล็อกอินผ่าน **Google OAuth2**
    - **Role-Based Access Control:** ใช้ Spring Security ร่วมกับ `@PreAuthorize` เพื่อกำหนดสิทธิ์การเข้าถึง API สำหรับ `ROLE_ADMIN` และ `ROLE_USER`

5.  **🏛️ สถาปัตยกรรมเชิงวัตถุ**
    - **Polymorphism:** ออกแบบ `Component` เป็น Abstract Class และมี Subclass สำหรับชิ้นส่วนแต่ละประเภท เพื่อให้ง่ายต่อการเพิ่มประเภทสินค้าใหม่ในอนาคต
    - **Separation of Concerns:** มีการแบ่งแยกหน้าที่ความรับผิดชอบของแต่ละคลาสอย่างชัดเจนตามหลักการออกแบบซอฟต์แวร์

### Frontend (React Applications)

#### 1. Admin Panel
1.  **📊 แดชบอร์ดสรุปข้อมูล**
    - แสดงผลข้อมูลทางธุรกิจด้วยกราฟจาก `Recharts`
    - มีฟังก์ชันสำหรับกรองข้อมูลตามช่วงเวลาและ Export Report (หมายเหตุ: ฟังก์ชัน Export ยังอยู่ในระหว่างการพัฒนาและอาจทำงานได้ไม่สมบูรณ์)

2.  **🎛️ ตารางจัดการข้อมูลแบบไดนามิก**
    - พัฒนาโดยใช้ `Tanstack React Table` เพื่อสร้างตารางข้อมูลที่มีฟังก์ชันการค้นหา, การจัดเรียง, และการแบ่งหน้า
    - **URL-Driven State:** สถานะของตารางจะถูกบันทึกไว้ใน URL Query Parameters ทำให้สามารถแชร์ลิงก์หรือ Bookmark ได้

3.  **📝 ส่วนจัดการคำสั่งซื้อ**
    - UI ในหน้า `OrderDetailPage` ถูกออกแบบมาให้สอดคล้องกับสถานะของคำสั่งซื้อจาก Backend
    - ผู้ดูแลระบบสามารถดำเนินการต่างๆ ได้ตามขั้นตอน เช่น การอนุมัติสลิป, การบันทึกข้อมูลการจัดส่ง, หรือการจัดการคืนเงิน

4.  **🖼️ ระบบจัดการและตัดรูปภาพ**
    - พัฒนา Component `ImageCropper` โดยใช้ `react-image-crop` และ HTML Canvas API เพื่อให้ผู้ใช้สามารถตัดรูปภาพได้ก่อนการอัปโหลด

5.  **🏗️ สถาปัตยกรรมแบบ Component-Based**
    - สร้าง UI Components ที่สามารถนำกลับมาใช้ใหม่ได้ทั่วทั้งโปรเจค เช่น `ReusableTable`, `ConfirmationModal`, `PageHeader`, `StatusBadge`
    - **Dynamic Form Generation:** ใช้ Object Configuration (`COMPONENT_CONFIG`) ในการสร้างฟอร์มสำหรับชิ้นส่วนแต่ละประเภทโดยอัตโนมัติ เพื่อลดการเขียนโค้ดซ้ำซ้อน

#### 2. User Panel
1.  **🖥️ UI จัดสเปคคอมพิวเตอร์อัจฉริยะ**
    - ส่วนติดต่อผู้ใช้ที่เรียบง่ายสำหรับการเลือกชิ้นส่วนคอมพิวเตอร์
    - แสดงผลการตรวจสอบความเข้ากันได้แบบ Real-time โดยไฮไลต์ชิ้นส่วนที่เข้ากันไม่ได้และแสดงเหตุผล

2.  **🔍 ระบบค้นหาและกรองสินค้า**
    - ผู้ใช้สามารถค้นหาสินค้าตามชื่อ, กรองตามประเภท, ยี่ห้อ, และคุณสมบัติอื่นๆ
    - จัดการสถานะการค้นหาด้วย `TanStack Query` เพื่อประสบการณ์การใช้งานที่รวดเร็ว

3.  **🛒 ตะกร้าสินค้าและกระบวนการสั่งซื้อ**
    - ระบบตะกร้าสินค้าที่ใช้งานง่าย
    - กระบวนการ Checkout ที่ปลอดภัยซึ่งเชื่อมต่อกับช่องทางการชำระเงินต่างๆ ที่ Backend รองรับ

4.  **👤 โปรไฟล์ผู้ใช้**
    - หน้าสำหรับผู้ใช้ในการจัดการข้อมูลส่วนตัวและดูประวัติการสั่งซื้อทั้งหมด

---

## 🛠️ เทคโนโลยีที่ใช้

| ส่วน | เทคโนโลยี | รายละเอียดและการใช้งาน |
|---|---|---|
| **Backend** | `Java 21`, `Spring Boot 3`, `Maven` | พัฒนา RESTful API และ Business Logic หลักของระบบ |
| **Frontend** | `React (Vite)`, `React Router` | ส่วนติดต่อผู้ใช้สำหรับ Admin Panel และ User Panel ในรูปแบบ Single Page Application |
| **Database** | `MongoDB` | ฐานข้อมูล NoSQL สำหรับจัดเก็บข้อมูลสินค้า, ผู้ใช้, และคำสั่งซื้อ |
| **Styling** | `React-Bootstrap`, `CSS Modules` | ออกแบบและจัดวางส่วนประกอบ UI |
| **State Mgt.** | `TanStack Query`, `React Context API` | ใช้ **TanStack Query** สำหรับจัดการ Server State ทั้งหมด (Fetching, Caching, Mutations) และใช้ **Context API** สำหรับจัดการสถานะการล็อกอินของผู้ใช้ |
| **Data Tables** | `Tanstack React Table` | สร้างตารางข้อมูลที่มีฟังก์ชันการทำงานขั้นสูง (ใช้ใน Admin Panel) |
| **Data Viz** | `Recharts` | แสดงผลข้อมูลในรูปแบบกราฟบนหน้า Dashboard (ใช้ใน Admin Panel) |
| **Authentication** | `Spring Security`, `JWT`, `Google OAuth2` | จัดการการยืนยันตัวตนและกำหนดสิทธิ์การเข้าถึง (RBAC) |
| **Cloud Storage** | `Amazon Web Services (AWS S3)` | จัดเก็บไฟล์รูปภาพและเอกสารต่างๆ |
| **Payment** | `PayPal REST API` | เชื่อมต่อระบบการชำระเงินออนไลน์ |
| **Deployment** | `Docker`, `Docker Compose` | สร้าง Container และจัดการการทำงานของ Services ทั้งหมด |

---

## 🚀 การติดตั้งและเริ่มต้นใช้งาน

### สิ่งที่ต้องมี
- Java JDK 21+
- Apache Maven
- Node.js & npm
- Docker & Docker Compose
- API Keys จาก AWS, PayPal Developer, และ Google Cloud Platform

### 1. ตั้งค่า Environment Variables
สร้างไฟล์ `.env` ที่ root ของโปรเจค และกำหนดค่าที่จำเป็น:

```env
# MongoDB Connection
SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/computers

# JWT Secret Key
JWT_SECRET_KEY=รหัสแบบสุ่มที่ยาวและแข็งแรง

# AWS S3 Bucket Credentials
AWS_ACCESS_KEY=รหัสกุญแจของ_Amazon_Webservice
AWS_SECRET_KEY=รหัสกุญแจลับของ_Amazon_Webservice

# PayPal Developer Credentials
PAYPAL_CLIENT_ID=รหัสกุญแจของ_Paypal_Client_Id
PAYPAL_CLIENT_SECRET=รหัสกุญแจลับของ_Paypal_Client_Secret

# Google OAuth2 Credentials
GOOGLE_CLIENT_ID=รหัสกุญแจของ_Google_Client_id_.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=รหัสกุญแจลับของ_Google_Client_Secret
```

### 2. การรันโปรเจคด้วย Docker
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/ThanDK/fullstack-pc-builder.git
    cd fullstack-pc-builder
    ```
2.  **Start all services:**
    ```bash
    docker-compose up --build
    ```
    - **Backend API** จะทำงานที่ `http://localhost:8080`
    - **Admin Panel** จะทำงานที่ `http://localhost:5173`
    - **User Panel** จะทำงานที่ `http://localhost:3000` (หรือพอร์ตอื่นที่กำหนดใน `docker-compose.yml`)
    - **MongoDB** จะทำงานภายใน Docker network

---

## 📁 โครงสร้างโปรเจค

```
/
├── AdminPanelX/          # Frontend (React) สำหรับ Admin
│   ├── public/
│   └── src/
│       ├── components/   # (Reusable UI Components)
│       ├── context/      # (AuthContext for global state)
│       ├── pages/        # (Each page of the application)
│       └── services/     # (API call functions)
├── User-ui/              # Frontend (React) สำหรับ User
│   ├── public/
│   └── src/
│       ├── api/          # (API call functions)
│       ├── assets/       # (Static assets)
│       ├── components/   # (Reusable UI Components)
│       ├── context/      # (Global state management)
│       ├── hook/         # (Custom hooks)
│       └── pages/        # (Each page of the application)
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

## 💡 สิ่งที่สามารถพัฒนาต่อได้

-   **[ ] Unit & Integration Tests:** เพิ่มการทดสอบในส่วนต่างๆ ของ Backend เพื่อเพิ่มความเสถียรของระบบ
-   **[ ] CI/CD Pipeline:** สร้าง Pipeline สำหรับการ Build และ Deploy อัตโนมัติเมื่อมีการ Push code
-   **[ ] Elasticsearch Integration:** เพิ่มประสิทธิภาพการค้นหาสินค้าด้วย Elasticsearch
-   **[ ] ระบบแจ้งเตือน (Notification System):** แจ้งเตือนผู้ดูแลระบบเมื่อมีออเดอร์ใหม่หรือมีเหตุการณ์สำคัญ
