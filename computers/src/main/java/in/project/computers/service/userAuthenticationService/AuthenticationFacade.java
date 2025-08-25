package in.project.computers.service.userAuthenticationService;

import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {

    /**
     * ดึงข้อมูลการยืนยันตัวตนของผู้ใช้ที่กำลังใช้งานระบบอยู่
     * @return อ็อบเจกต์ {@link Authentication} ที่มีข้อมูลของผู้ใช้ เช่น username, authorities, และสถานะการยืนยันตัวตน
     */
    Authentication getAuthentication();

}