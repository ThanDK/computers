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
     * <p><b>การทำงาน:</b></p>
     * <ul>
     *     <li>1. รับข้อมูลผู้ใช้ (name, email, password) จาก Request Body</li>
     *     <li>2. ส่งข้อมูลต่อไปยัง `UserService` ซึ่งจะรับผิดชอบการตรวจสอบว่าอีเมลซ้ำหรือไม่,
     *        ทำการเข้ารหัสรหัสผ่าน (Password Hashing), และบันทึกข้อมูลผู้ใช้ใหม่ลงในฐานข้อมูล</li>
     *     <li>3. คืนค่าเป็น `UserResponse` ซึ่งเป็นข้อมูลผู้ใช้ที่ไม่มีรหัสผ่าน เพื่อความปลอดภัย</li>
     * </ul>
     * <p><b>ตัวอย่าง Request Body (JSON):</b></p>
     * <pre>{@code
     * {
     *   "name": "สมชาย ใจดี",
     *   "email": "somchai.j@example.com",
     *   "password": "MyStrongPassword123!"
     * }
     * }</pre>
     * <p><b>ตัวอย่าง Response Body (JSON) เมื่อสำเร็จ:</b></p>
     * <pre>{@code
     * {
     *   "id": "user_a1b2c3d4",
     *   "name": "สมชาย ใจดี",
     *   "email": "somchai.j@example.com",
     *   "role": "USER"
     * }
     * }</pre>
     *
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