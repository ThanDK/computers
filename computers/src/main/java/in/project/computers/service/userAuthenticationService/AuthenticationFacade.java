package in.project.computers.service.userAuthenticationService;

import org.springframework.security.core.Authentication;

/**
 * Interface ที่ทำหน้าที่เป็น Facade Pattern เพื่อช่วยให้การเข้าถึงข้อมูล Authentication ของผู้ใช้ที่ล็อกอินอยู่ง่ายขึ้น.
 * <p>
 * การใช้ Facade นี้ช่วยลดการผูกมัด (coupling) กับคลาส {@code SecurityContextHolder} โดยตรง,
 * ทำให้สามารถทดสอบ (mock) การยืนยันตัวตนใน Unit Test ได้สะดวกยิ่งขึ้น.
 */
public interface AuthenticationFacade {

    /**
     * ดึงข้อมูลการยืนยันตัวตน (Authentication) ของผู้ใช้ที่กำลังใช้งานระบบอยู่.
     *
     * @return อ็อบเจกต์ {@link Authentication} ที่มีข้อมูลของผู้ใช้ เช่น username (email),
     *         authorities (roles), และสถานะการยืนยันตัวตน.
     */
    Authentication getAuthentication();

}