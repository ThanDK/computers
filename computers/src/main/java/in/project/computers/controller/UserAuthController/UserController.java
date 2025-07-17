package in.project.computers.controller.UserAuthController;

import in.project.computers.dto.user.UserRequest;
import in.project.computers.dto.user.UserResponse;
import in.project.computers.service.userAuthenticationService.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h3>User Controller</h3>
 * <p>
 * Controller สำหรับจัดการ API Endpoints ที่เกี่ยวกับการกระทำของผู้ใช้ทั่วไป
 * ซึ่งเป็น Public และไม่ต้องการการยืนยันตัวตน (Authentication) เช่น การลงทะเบียน
 * </p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // ใช้ RequiredArgsConstructor เพื่อความปลอดภัยและเป็น Best Practice
@Slf4j
@CrossOrigin("*") // อนุญาตการเข้าถึงจากทุก Origin (ควรปรับแก้ใน Production)
public class UserController {

    private final UserService userService;

    /**
     * <h4>[POST] /api/register</h4>
     * <p>Endpoint สำหรับการลงทะเบียนผู้ใช้ใหม่</p>
     * @param request DTO ที่มีข้อมูลสำหรับลงทะเบียน
     * @return ResponseEntity ที่มี UserResponse ของผู้ใช้ที่เพิ่งสร้าง พร้อมสถานะ 201 CREATED
     * @throws org.springframework.web.server.ResponseStatusException หากอีเมลที่ใช้ลงทะเบียนมีอยู่แล้วในระบบ (จะคืนสถานะ 409 CONFLICT)
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        log.info("New user registration attempt for email: {}", request.getEmail());
        UserResponse registeredUser = userService.registerUser(request);
        // การคืนค่าด้วย ResponseEntity ช่วยให้เราควบคุม Header และ Status Code ได้อย่างเต็มที่
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }


}