package in.project.computers.service.userAuthenticationService.mapper;

import in.project.computers.DTO.user.userRequest.AdminUserRequest;
import in.project.computers.DTO.user.userRequest.UserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import in.project.computers.entity.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * A dedicated component responsible for mapping between User DTOs and the UserEntity.
 * This follows the Single Responsibility Principle, separating data conversion logic
 * from business logic in the UserService.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PasswordEncoder passwordEncoder;


    public UserEntity convertToEntity(UserRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role("ROLE_USER")
                .locked(false)
                .build();
    }


    public UserEntity convertAdminUserToEntity(AdminUserRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole()) // Role is set from the admin request
                .locked(false)    // Default to unlocked
                .build();
    }


    public UserResponse convertToResponse(UserEntity userEntity) {
        return UserResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .email(userEntity.getEmail())
                .role(userEntity.getRole())
                .profilePictureUrl(userEntity.getProfilePictureUrl())
                .locked(userEntity.isLocked())
                .build();
    }
}