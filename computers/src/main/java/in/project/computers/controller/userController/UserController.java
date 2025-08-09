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
 * Controller สำหรับจัดการ Endpoint ที่ผู้ใช้ทั่วไปสามารถเข้าถึงได้ เช่น การลงทะเบียน
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * ลงทะเบียนผู้ใช้ใหม่
     * @param request ข้อมูลสำหรับลงทะเบียน (email, password, name)
     * @return ข้อมูลผู้ใช้ที่ลงทะเบียนสำเร็จ (HttpStatus 201)
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        log.info("New user registration attempt for email: {}", request.getEmail());
        UserResponse registeredUser = userService.registerUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }
}