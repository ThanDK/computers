package in.project.computers.service.userAuthenticationService;

import in.project.computers.DTO.user.userRequest.AdminUserRequest;
import in.project.computers.DTO.user.userRequest.UserProfileUpdateRequest;
import in.project.computers.DTO.user.userRequest.UserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    UserResponse registerUser(UserRequest request);

    UserResponse AdminCreateUser(AdminUserRequest request);

    String findByUserId();

    List<UserResponse> getAllUsers();

    UserResponse updateUserByAdmin(String userId, AdminUserRequest request);

    UserResponse getUserById(String userId);

    void deleteUser(String userId);

    UserResponse updateUserProfile(UserProfileUpdateRequest request, MultipartFile file);

    UserResponse removeUserProfilePicture();

    UserResponse lockUser(String userId);

    UserResponse unlockUser(String userId);

    UserResponse getUserByEmail(String email);
}