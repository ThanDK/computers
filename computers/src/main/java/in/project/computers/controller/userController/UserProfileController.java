package in.project.computers.controller.userController;

import in.project.computers.DTO.user.userRequest.UserProfileUpdateRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import in.project.computers.service.userAuthenticationService.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller สำหรับจัดการโปรไฟล์ส่วนตัวของผู้ใช้ที่ล็อกอินอยู่
 * <p>
 * <b>คำเตือน:</b> {@code @CrossOrigin("*")} ไม่ปลอดภัยสำหรับ Production ควรระบุ Origin ของ Frontend ให้ชัดเจน
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserProfileController {

    private final UserService userService;

    /**
     * ดึงข้อมูลโปรไฟล์ของผู้ใช้ที่ล็อกอินอยู่ปัจจุบัน
     * @param authentication ข้อมูลการยืนยันตัวตน (Spring Security inject ให้)
     * @return ข้อมูลโปรไฟล์ของผู้ใช้
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        log.info("Fetching profile for user: {}", authentication.getName());
        UserResponse user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(user);
    }

    /**
     * อัปเดตข้อมูลโปรไฟล์และรูปภาพของผู้ใช้ (รับข้อมูลแบบ multipart/form-data)
     * @param request ข้อมูลโปรไฟล์ที่ต้องการอัปเดต (part: "profileData")
     * @param file รูปภาพโปรไฟล์ใหม่ (optional, part: "file")
     * @return ข้อมูลโปรไฟล์หลังการอัปเดต
     */
    @PutMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<UserResponse> updateUserProfile(
            @RequestPart("profileData") @Valid UserProfileUpdateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        log.info("User is updating their profile with combined data.");
        UserResponse updatedUser = userService.updateUserProfile(request, file);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * ลบรูปภาพโปรไฟล์ของผู้ใช้
     * @return ข้อมูลโปรไฟล์หลังการอัปเดต (ไม่มีรูปภาพ)
     */
    @DeleteMapping("/picture")
    public ResponseEntity<UserResponse> removeUserProfilePicture() {
        log.info("User is removing their profile picture.");
        UserResponse updatedUser = userService.removeUserProfilePicture();
        return ResponseEntity.ok(updatedUser);
    }

}