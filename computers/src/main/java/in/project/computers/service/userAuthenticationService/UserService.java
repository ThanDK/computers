package in.project.computers.service.userAuthenticationService;

import in.project.computers.DTO.user.userRequest.AdminUserRequest;
import in.project.computers.DTO.user.userRequest.UserProfileUpdateRequest;
import in.project.computers.DTO.user.userRequest.UserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    /**
     * ลงทะเบียนผู้ใช้ใหม่
     * @param request ข้อมูลที่จำเป็นสำหรับการลงทะเบียน
     * @return ข้อมูลผู้ใช้ที่ลงทะเบียนสำเร็จ
     */
    UserResponse registerUser(UserRequest request);

    /**
     * สร้างผู้ใช้ใหม่โดยผู้ดูแลระบบ
     * <p>
     * อนุญาตให้ผู้ดูแลระบบสร้างผู้ใช้พร้อมกำหนด Role และสถานะต่างๆ ได้
     * </p>
     * @param request ข้อมูลผู้ใช้ที่ต้องการสร้าง
     * @return ข้อมูลผู้ใช้ที่สร้างสำเร็จ
     */
    UserResponse AdminCreateUser(AdminUserRequest request);

    /**
     * ค้นหา ID ของผู้ใช้ที่กำลังล็อกอินอยู่
     * <p>
     * เป็นเมธอดช่วยเหลือที่ใช้ภายใน Service อื่นๆ เพื่อระบุตัวตนผู้ใช้จาก Security Context
     * </p>
     * @return ID ของผู้ใช้ที่ล็อกอินอยู่
     */
    String findByUserId();

    /**
     * ดึงรายชื่อผู้ใช้ทั้งหมดในระบบ
     * @return List ของข้อมูลผู้ใช้ทั้งหมด
     */
    List<UserResponse> getAllUsers();

    /**
     * อัปเดตข้อมูลผู้ใช้โดยผู้ดูแลระบบ
     * @param userId  ID ของผู้ใช้ที่ต้องการอัปเดต
     * @param request ข้อมูลใหม่ที่ต้องการอัปเดต
     * @return ข้อมูลผู้ใช้ที่อัปเดตแล้ว
     */
    UserResponse updateUserByAdmin(String userId, AdminUserRequest request);

    /**
     * ดึงข้อมูลผู้ใช้ตาม ID
     *
     * @param userId ID ของผู้ใช้ที่ต้องการค้นหา
     * @return ข้อมูลของผู้ใช้
     */
    UserResponse getUserById(String userId);

    /**
     * ลบผู้ใช้ออกจากระบบ
     *
     * @param userId ID ของผู้ใช้ที่ต้องการลบ
     */
    void deleteUser(String userId);

    /**
     * อัปเดตโปรไฟล์ส่วนตัวของผู้ใช้ที่ล็อกอินอยู่
     * <p>
     * ผู้ใช้สามารถอัปเดตชื่อ และ/หรือ รูปโปรไฟล์ของตนเองได้
     * </p>
     * @param request ข้อมูลโปรไฟล์ที่ต้องการอัปเดต
     * @param file    ไฟล์รูปภาพใหม่
     * @return ข้อมูลโปรไฟล์ที่อัปเดตแล้ว
     */
    UserResponse updateUserProfile(UserProfileUpdateRequest request, MultipartFile file);

    /**
     * ลบรูปโปรไฟล์ของผู้ใช้ที่ล็อกอินอยู่
     *
     * @return ข้อมูลโปรไฟล์ที่อัปเดตแล้ว
     */
    UserResponse removeUserProfilePicture();

    /**
     * ล็อกบัญชีผู้ใช้
     * <p>
     * ผู้ใช้ที่ถูกล็อกจะไม่สามารถล็อกอินเข้าสู่ระบบได้
     * </p>
     * @param userId ID ของผู้ใช้ที่ต้องการล็อก
     * @return ข้อมูลผู้ใช้ที่สถานะถูกเปลี่ยนเป็น "LOCKED"
     */
    UserResponse lockUser(String userId);

    /**
     * ปลดล็อกบัญชีผู้ใช้
     * @param userId ID ของผู้ใช้ที่ต้องการปลดล็อก
     * @return ข้อมูลผู้ใช้ที่สถานะถูกเปลี่ยนเป็น "ACTIVE"
     */
    UserResponse unlockUser(String userId);

    /**
     * ดึงข้อมูลผู้ใช้ด้วย Email
     * @param email Email ของผู้ใช้ที่ต้องการค้นหา
     * @return ข้อมูลของผู้ใช้
     */
    UserResponse getUserByEmail(String email);
}