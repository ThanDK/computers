package in.project.computers.controller.userController;

import in.project.computers.DTO.user.userRequest.AuthenticationRequest;
import in.project.computers.DTO.user.userResponse.AuthenticationResponse;
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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    /**
     * ล็อกอินเข้าสู่ระบบด้วย Email และ Password เพื่อขอรับ JWT
     * <p>
     * Endpoint นี้จะรับ Email และ Password, ตรวจสอบความถูกต้องผ่าน Spring Security's AuthenticationManager,
     * และหากสำเร็จ จะสร้าง JWT token ส่งกลับไปให้ Client
     * @param request ข้อมูลสำหรับล็อกอิน email, password
     * @return AuthenticationResponse ที่มี JWT token
     * @throws BadCredentialsException หากข้อมูลล็อกอินไม่ถูกต้อง ส่งผลให้เกิด HTTP 401 Unauthorized
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        log.info("Authentication attempt for user: {}", request.getEmail());
        try {
            // ตรวจสอบ Credential กับ Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed authentication attempt for user: {}", request.getEmail());
            throw e; // ส่ง 401 Unauthorized กลับไปโดยอัตโนมัติจาก Exception Handler ของ Spring Security
        }

        // ถ้า authenticate ผ่าน, ดำเนินการสร้าง Token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        final String jwtToken = jwtUtil.generateToken(userDetails);

        log.info("User '{}' authenticated successfully. JWT generated.", userDetails.getUsername());
        AuthenticationResponse response = new AuthenticationResponse(request.getEmail(), jwtToken);

        return ResponseEntity.ok(response);
    }
}