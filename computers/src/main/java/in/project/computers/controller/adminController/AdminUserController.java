package in.project.computers.controller.adminController;

import in.project.computers.DTO.user.userRequest.AdminUserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import in.project.computers.service.userAuthenticationService.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /**
     * สร้างบัญชีผู้ใช้ใหม่
     * <p>
     * Endpoint นี้อนุญาตให้ผู้ดูแลระบบสร้างบัญชีผู้ใช้ใหม่ พร้อมทั้งกำหนด Role และสถานะเริ่มต้นได้โดยตรง
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param request อ็อบเจกต์ {@link AdminUserRequest} ที่มีข้อมูลผู้ใช้ใหม่ เช่น email, password, และ roles
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ของผู้ใช้ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUserByAdmin(@Valid @RequestBody AdminUserRequest request) {
        log.info("Admin creating a new user with email: {}", request.getEmail());
        UserResponse registeredUser = userService.AdminCreateUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    /**
     * ดึงรายชื่อผู้ใช้ทั้งหมดในระบบ
     * <p>
     * Endpoint นี้สำหรับผู้ดูแลระบบเพื่อดูภาพรวมของผู้ใช้ทั้งหมดที่มีในระบบ
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link UserResponse} และสถานะ 200 OK
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Admin request to get all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * ดึงข้อมูลผู้ใช้ตาม ID ที่ระบุ
     * <p>
     * Endpoint นี้อนุญาตให้ผู้ดูแลระบบเข้าถึงข้อมูลโปรไฟล์ของผู้ใช้คนใดก็ได้โดยตรงผ่าน ID
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param userId ID ของผู้ใช้ที่ต้องการดึงข้อมูล (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ของผู้ใช้และสถานะ 200 OK
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        log.info("Admin request to get user by ID: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * อัปเดตข้อมูลผู้ใช้
     * <p>
     * Endpoint นี้สำหรับผู้ดูแลระบบเพื่อแก้ไขข้อมูลของผู้ใช้ที่มีอยู่แล้ว เช่น การเปลี่ยนชื่อ, email หรือ roles
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param userId ID ของผู้ใช้ที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link AdminUserRequest} ที่มีข้อมูลใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserByAdmin(@PathVariable String userId, @Valid @RequestBody AdminUserRequest request) {
        log.info("Admin request to update user by ID: {}", userId);
        UserResponse updatedUser = userService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * ลบบัญชีผู้ใช้
     * <p>
     * Endpoint นี้ใช้สำหรับลบบัญชีผู้ใช้ออกจากระบบอย่างถาวร เมื่อดำเนินการสำเร็จจะคืนสถานะ 204 No Content
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param userId ID ของผู้ใช้ที่ต้องการลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Admin request to delete user by ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ล็อกบัญชีผู้ใช้
     * <p>
     * Endpoint นี้ใช้สำหรับระงับการใช้งานบัญชีผู้ใช้ ทำให้ผู้ใช้คนดังกล่าวไม่สามารถล็อกอินเข้าสู่ระบบได้
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param userId ID ของผู้ใช้ที่ต้องการล็อก (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ที่อัปเดตสถานะเป็น LOCKED และสถานะ 200 OK
     */
    @PutMapping("/lock/{userId}")
    public ResponseEntity<UserResponse> lockUser(@PathVariable String userId) {
        log.info("Admin request to LOCK user account: {}", userId);
        UserResponse lockedUser = userService.lockUser(userId);
        return ResponseEntity.ok(lockedUser);
    }

    /**
     * ปลดล็อกบัญชีผู้ใช้
     * <p>
     * Endpoint นี้ใช้สำหรับเปิดใช้งานบัญชีผู้ใช้ที่ถูกล็อกไว้ก่อนหน้า ให้สามารถกลับมาล็อกอินได้อีกครั้ง
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param userId ID ของผู้ใช้ที่ต้องการปลดล็อก (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ที่อัปเดตสถานะเป็น ACTIVE และสถานะ 200 OK
     */
    @PutMapping("/unlock/{userId}")
    public ResponseEntity<UserResponse> unlockUser(@PathVariable String userId) {
        log.info("Admin request to UNLOCK user account: {}", userId);
        UserResponse unlockedUser = userService.unlockUser(userId);
        return ResponseEntity.ok(unlockedUser);
    }
}