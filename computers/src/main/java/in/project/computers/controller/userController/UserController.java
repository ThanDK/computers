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

/**
 * Controller สำหรับจัดการ Endpoint ที่ผู้ใช้ทั่วไปสามารถเข้าถึงได้โดยไม่ต้องยืนยันตัวตน
 * <p>
 * หน้าที่หลักคือการลงทะเบียนผู้ใช้ใหม่ (Register)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * ลงทะเบียนผู้ใช้ใหม่เข้าสู่ระบบ
     * @param request ข้อมูลที่จำเป็นสำหรับการลงทะเบียน ประกอบด้วย email, password, และชื่อ
     * @return ข้อมูลของผู้ใช้ที่ลงทะเบียนสำเร็จ (ไม่รวมรหัสผ่าน) พร้อม HttpStatus 201 CREATED
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        log.info("New user registration attempt for email: {}", request.getEmail());
        UserResponse registeredUser = userService.registerUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }
}