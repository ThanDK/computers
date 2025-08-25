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
 * ให้ผู้ใช้สามารถดู, อัปเดตข้อมูลส่วนตัว, และจัดการรูปโปรไฟล์ได้
 * ทุก Endpoint ในคลาสนี้ต้องการการยืนยันตัวตน (Authentication)
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
     * @param authentication ข้อมูลการยืนยันตัวตนที่ถูก inject โดย Spring Security
     * @return ข้อมูลโปรไฟล์ของผู้ใช้ (UserResponse)
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        log.info("Fetching profile for user: {}", authentication.getName());
        UserResponse user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(user);
    }

    /**
     * อัปเดตข้อมูลโปรไฟล์ (เช่น ชื่อ) และ/หรือรูปภาพของผู้ใช้
     * <p>
     * รับข้อมูลแบบ multipart/form-data เพื่อให้สามารถส่งข้อมูล JSON และไฟล์รูปภาพได้พร้อมกัน
     * @param request ข้อมูลโปรไฟล์ที่ต้องการอัปเดต (JSON ใน part ที่ชื่อ "profileData")
     * @param file รูปภาพโปรไฟล์ใหม่ (เป็นทางเลือก, ใน part ที่ชื่อ "file")
     * @return ข้อมูลโปรไฟล์ของผู้ใช้หลังการอัปเดต
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
     * ลบรูปภาพโปรไฟล์ของผู้ใช้ที่ล็อกอินอยู่
     * @return ข้อมูลโปรไฟล์ของผู้ใช้หลังการอัปเดต (ซึ่งจะไม่มี URL ของรูปภาพ)
     */
    @DeleteMapping("/picture")
    public ResponseEntity<UserResponse> removeUserProfilePicture() {
        log.info("User is removing their profile picture.");
        UserResponse updatedUser = userService.removeUserProfilePicture();
        return ResponseEntity.ok(updatedUser);
    }

}