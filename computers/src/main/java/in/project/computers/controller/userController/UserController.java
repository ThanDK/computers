package in.project.computers.controller.userController;

import in.project.computers.DTO.user.userRequest.UserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import in.project.computers.service.userAuthenticationService.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * ลงทะเบียนผู้ใช้ใหม่เข้าสู่ระบบ
     * <p>
     * Endpoint นี้เป็นสาธารณะสำหรับให้ผู้ใช้ใหม่สามารถสร้างบัญชีได้ โดยรับข้อมูลที่จำเป็นสำหรับการลงทะเบียน
     * และจะส่งคืนข้อมูลโปรไฟล์ของผู้ใช้ที่สร้างสำเร็จ (ไม่รวมรหัสผ่าน)
     * </p>
     * @param request อ็อบเจกต์ {@link UserRequest} ที่มีข้อมูล email, password, และชื่อ
     * @return ResponseEntity ที่มีข้อมูล {@link UserResponse} ของผู้ใช้ที่ลงทะเบียนสำเร็จและสถานะ 201 Created
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        log.info("New user registration attempt for email: {}", request.getEmail());
        UserResponse registeredUser = userService.registerUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }
}