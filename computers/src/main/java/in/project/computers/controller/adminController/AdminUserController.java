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
@PreAuthorize("hasRole('ADMIN')") // Secures all endpoints in this controller
public class AdminUserController {

    // IMPORTANT: 'final' is needed for @RequiredArgsConstructor to work
    private final UserService userService;

    /**
     * [Admin] Creates a new user with a specified role.
     * POST /api/admin/users
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUserByAdmin(@Valid @RequestBody AdminUserRequest request) {
        log.info("Admin creating a new user with email: {}", request.getEmail());
        UserResponse registeredUser = userService.AdminCreateUser(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    /**
     * [Admin] Gets a list of all users.
     * GET /api/admin/users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Admin request to get all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * [Admin] Gets a single user by their ID.
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        log.info("Admin request to get user by ID: {}", userId);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * [Admin] Updates a user's information by their ID.
     * PUT /api/admin/users/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserByAdmin(@PathVariable String userId, @Valid @RequestBody AdminUserRequest request) {
        log.info("Admin request to update user by ID: {}", userId);
        UserResponse updatedUser = userService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * [Admin] Deletes a user by their ID.
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Admin request to delete user by ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}