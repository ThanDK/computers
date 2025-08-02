package in.project.computers.service.userAuthenticationService;

import in.project.computers.DTO.user.userRequest.AdminUserRequest;
import in.project.computers.DTO.user.userRequest.UserProfileUpdateRequest; // Import the new DTO
import in.project.computers.DTO.user.userRequest.UserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;

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