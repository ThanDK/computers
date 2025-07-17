package in.project.computers.service.userAuthenticationService;

import in.project.computers.dto.user.AdminUserRequest;
import in.project.computers.dto.user.UserProfileUpdateRequest; // Import the new DTO
import in.project.computers.dto.user.UserRequest;
import in.project.computers.dto.user.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse registerUser(UserRequest request);

    UserResponse AdminCreateUser(AdminUserRequest request);

    String findByUserId();


    List<UserResponse> getAllUsers();


    UserResponse updateUserByAdmin(String userId, AdminUserRequest request);


    UserResponse updateUserProfile(UserProfileUpdateRequest request);


    UserResponse getUserById(String userId);


    void deleteUser(String userId);
}