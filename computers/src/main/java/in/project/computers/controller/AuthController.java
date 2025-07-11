package in.project.computers.controller;

import in.project.computers.dto.user.AuthenticationRequest;
import in.project.computers.dto.user.AuthenticationResponse;
import in.project.computers.service.userAuthenticationService.AppUserDetailsService;
import in.project.computers.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * <h3>Authentication Controller</h3>
 * <p>
 * Controller สำหรับจัดการกระบวนการยืนยันตัวตน (Authentication) ของระบบ
 * รับผิดชอบ Endpoint สำหรับการล็อกอิน (Login) โดยเฉพาะ
 * เมื่อผู้ใช้ส่ง Email และ Password ที่ถูกต้อง, ระบบจะตรวจสอบและออก JSON Web Token (JWT)
 * เพื่อใช้ในการยืนยันตัวตนในคำขอ (Request) ต่อๆ ไป
 * </p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    /**
     * <h4>[POST] /api/login</h4>
     * <p>Endpoint สำหรับการล็อกอินเข้าสู่ระบบ</p>
     * <p><b>การทำงาน:</b></p>
     * <ul>
     *     <li>1. รับ `email` และ `password` จาก Request Body</li>
     *     <li>2. ใช้ `AuthenticationManager` ของ Spring Security เพื่อตรวจสอบว่าข้อมูลที่ส่งมาถูกต้องหรือไม่
     *        (ถ้าไม่ถูกต้อง Spring Security จะโยน `BadCredentialsException` และคืนสถานะ 401 UNAUTHORIZED โดยอัตโนมัติ)</li>
     *     <li>3. หากตรวจสอบผ่าน, จะดึงข้อมูล `UserDetails` (รวมถึง Roles) ของผู้ใช้ออกมา</li>
     *     <li>4. ใช้ `JwtUtil` เพื่อสร้าง JWT Token ซึ่งจะฝังข้อมูลผู้ใช้และ Roles ไว้ข้างใน</li>
     *     <li>5. คืนค่า Token กลับไปให้ Client ในรูปแบบของ `AuthenticationResponse`</li>
     * </ul>
     * <p><b>ตัวอย่าง Request Body (JSON):</b></p>
     * <pre>{@code
     * {
     *   "email": "user@example.com",
     *   "password": "password123"
     * }
     * }</pre>
     * <p><b>ตัวอย่าง Response Body (JSON):</b></p>
     * <pre>{@code
     * {
     *   "email": "user@example.com",
     *   "jwt": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMi..."
     * }
     * }</pre>
     *
     * @param request DTO ที่มี email และ password ของผู้ใช้
     * @return AuthenticationResponse ที่มี JWT token สำหรับการใช้งานต่อไป
     * @throws BadCredentialsException หาก email หรือ password ไม่ถูกต้อง
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        log.info("Authentication attempt for user: {}", request.getEmail());
        try {
            // ขั้นตอนการตรวจสอบ Credential
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed authentication attempt for user: {}", request.getEmail());
            // สามารถโยน Exception ให้ GlobalExceptionHandler จัดการ หรือจะ return ResponseEntity โดยตรงก็ได้
            throw e; // โยน Exception เพื่อให้ Spring จัดการและคืนสถานะ 401
        }

        // ถ้า authenticate ผ่าน, ดำเนินการสร้าง Token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        final String jwtToken = jwtUtil.generateToken(userDetails);

        log.info("User '{}' authenticated successfully. JWT generated.", userDetails.getUsername());
        AuthenticationResponse response = new AuthenticationResponse(request.getEmail(), jwtToken);

        return ResponseEntity.ok(response);
    }
}