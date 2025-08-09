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

/**
 * Controller สำหรับจัดการข้อมูลผู้ใช้ (User) ซึ่งต้องใช้สิทธิ์ Admin เท่านั้น
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /**
     * สร้างผู้ใช้ใหม่โดย Admin
     * @param request ข้อมูลผู้ใช้ใหม่ (email, password, roles)
     * @return ข้อมูลผู้ใช้ที่สร้างสำเร็จ (HttpStatus 201)
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUserByAdmin(@Valid @RequestBody AdminUserRequest request) {
        log.info("Admin creating a new user with email: {}", request.getEmail());
        UserResponse registeredUser = userService.AdminCreateUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    /**
     * ดึงรายชื่อผู้ใช้ทั้งหมดในระบบ
     * @return List ของผู้ใช้ทั้งหมด
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Admin request to get all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * ดึงข้อมูลผู้ใช้ตาม ID
     * @param userId ID ของผู้ใช้ที่ต้องการ
     * @return ข้อมูลผู้ใช้ที่ค้นพบ
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        log.info("Admin request to get user by ID: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * อัปเดตข้อมูลผู้ใช้โดย Admin
     * @param userId ID ของผู้ใช้ที่จะอัปเดต
     * @param request ข้อมูลใหม่ของผู้ใช้
     * @return ข้อมูลผู้ใช้หลังอัปเดต
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserByAdmin(@PathVariable String userId, @Valid @RequestBody AdminUserRequest request) {
        log.info("Admin request to update user by ID: {}", userId);
        UserResponse updatedUser = userService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * ลบผู้ใช้ (คืนค่า 204 No Content)
     * @param userId ID ของผู้ใช้ที่จะลบ
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Admin request to delete user by ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ล็อกบัญชีผู้ใช้ (ทำให้ไม่สามารถล็อกอินได้)
     * @param userId ID ของผู้ใช้ที่จะล็อก
     * @return ข้อมูลผู้ใช้ที่ถูกล็อก
     */
    @PutMapping("/lock/{userId}")
    public ResponseEntity<UserResponse> lockUser(@PathVariable String userId) {
        log.info("Admin request to LOCK user account: {}", userId);
        UserResponse lockedUser = userService.lockUser(userId);
        return ResponseEntity.ok(lockedUser);
    }

    /**
     * ปลดล็อกบัญชีผู้ใช้
     * @param userId ID ของผู้ใช้ที่จะปลดล็อก
     * @return ข้อมูลผู้ใช้ที่ถูกปลดล็อก
     */
    @PutMapping("/unlock/{userId}")
    public ResponseEntity<UserResponse> unlockUser(@PathVariable String userId) {
        log.info("Admin request to UNLOCK user account: {}", userId);
        UserResponse unlockedUser = userService.unlockUser(userId);
        return ResponseEntity.ok(unlockedUser);
    }
}