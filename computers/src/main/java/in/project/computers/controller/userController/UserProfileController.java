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

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserProfileController {

    private final UserService userService;

    /**
     * ดึงข้อมูลโปรไฟล์ของผู้ใช้ที่ล็อกอินอยู่
     * <p>
     * Endpoint นี้สำหรับให้ผู้ใช้ที่ล็อกอินแล้ว ดึงข้อมูลโปรไฟล์ล่าสุดของตนเอง
     * </p>
     * @param authentication ข้อมูลการยืนยันตัวตนที่ถูก inject โดย Spring Security เพื่อระบุตัวตนผู้ใช้
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ของผู้ใช้และสถานะ 200 OK
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        log.info("Fetching profile for user: {}", authentication.getName());
        UserResponse user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(user);
    }

    /**
     * อัปเดตข้อมูลและ/หรือรูปภาพโปรไฟล์
     * <p>
     * Endpoint นี้รับข้อมูลแบบ multipart/form-data เพื่อให้ผู้ใช้สามารถอัปเดตข้อมูลโปรไฟล์ (เช่น ชื่อ)
     * และ/หรืออัปโหลดรูปภาพโปรไฟล์ใหม่ได้ในคำขอเดียว
     * </p>
     * @param request ข้อมูลโปรไฟล์ที่ต้องการอัปเดต (JSON ใน part ที่ชื่อ "profileData")
     * @param file รูปภาพโปรไฟล์ใหม่ (เป็นทางเลือก, ใน part ที่ชื่อ "file")
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ที่อัปเดตแล้วและสถานะ 200 OK
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
     * <p>
     * Endpoint นี้สำหรับให้ผู้ใช้ลบรูปภาพโปรไฟล์ปัจจุบันของตนเองออก
     * </p>
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ที่อัปเดตแล้ว (ซึ่งจะไม่มี URL ของรูปภาพ) และสถานะ 200 OK
     */
    @DeleteMapping("/picture")
    public ResponseEntity<UserResponse> removeUserProfilePicture() {
        log.info("User is removing their profile picture.");
        UserResponse updatedUser = userService.removeUserProfilePicture();
        return ResponseEntity.ok(updatedUser);
    }

}