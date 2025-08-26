package in.project.computers.service.userAuthenticationService.mapper;

import in.project.computers.DTO.user.userRequest.AdminUserRequest;
import in.project.computers.DTO.user.userRequest.UserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import in.project.computers.entity.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
                .role(request.getRole())
                .locked(false)
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