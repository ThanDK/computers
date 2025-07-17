package in.project.computers.controller.adminController;

import in.project.computers.dto.user.AdminUserRequest;
import in.project.computers.dto.user.UserResponse;
import in.project.computers.service.userAuthenticationService.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {


    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUserByAdmin(@Valid @RequestBody AdminUserRequest request) {
        log.info("Admin creating a new user with email: {}", request.getEmail());
        UserResponse registeredUser = userService.AdminCreateUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Admin request to get all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        log.info("Admin request to get user by ID: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserByAdmin(@PathVariable String userId, @Valid @RequestBody AdminUserRequest request) {
        log.info("Admin request to update user by ID: {}", userId);
        UserResponse updatedUser = userService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(updatedUser);
    }


    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Admin request to delete user by ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}