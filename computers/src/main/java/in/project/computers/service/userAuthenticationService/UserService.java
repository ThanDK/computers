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

    // --- NEW METHODS ---

    /**
     * Retrieves a list of all users. (Admin only)
     * @return A list of UserResponse objects.
     */
    List<UserResponse> getAllUsers();

    /**
     * Updates a user's details by their ID. (Admin only)
     * @param userId The ID of the user to update.
     * @param request The request DTO containing the new data.
     * @return The updated user's details as a UserResponse.
     */
    UserResponse updateUserByAdmin(String userId, AdminUserRequest request);

    /**
     * Allows a logged-in user to update their own profile.
     * @param request The request DTO containing the new data (e.g., name, password).
     * @return The updated user's details as a UserResponse.
     */
    UserResponse updateUserProfile(UserProfileUpdateRequest request);

    /**
     * Retrieves a single user by their ID. (Admin only)
     * @param userId The ID of the user to retrieve.
     * @return The user's details as a UserResponse.
     */
    UserResponse getUserById(String userId);

    /**
     * Deletes a user by their ID. (Admin only)
     * @param userId The ID of the user to delete.
     */
    void deleteUser(String userId);
}