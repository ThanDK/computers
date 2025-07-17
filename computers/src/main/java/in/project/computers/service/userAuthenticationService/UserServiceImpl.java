package in.project.computers.service.userAuthenticationService;

import in.project.computers.dto.user.AdminUserRequest;
import in.project.computers.dto.user.UserProfileUpdateRequest;
import in.project.computers.dto.user.UserRequest;
import in.project.computers.dto.user.UserResponse;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalRepo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service สำหรับจัดการตรรกะที่เกี่ยวข้องกับผู้ใช้ เช่น การลงทะเบียน, การอัปเดต
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public UserResponse registerUser(UserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email " + request.getEmail() + " Already Exists");
        }
        UserEntity newUser = convertToEntity(request);
        newUser = userRepository.save(newUser);
        return convertToResponse(newUser);
    }

    @Override
    public UserResponse AdminCreateUser(AdminUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email " + request.getEmail() + " Already Exists");
        }
        UserEntity newUser = convertAdminUserToEntity(request);
        newUser = userRepository.save(newUser);
        return convertToResponse(newUser);
    }

    @Override
    public String findByUserId() {
        String loggedInUserEmail = authenticationFacade.getAuthentication().getName();
        UserEntity loggedInUser = userRepository.findByEmail(loggedInUserEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Not Found User " + loggedInUserEmail));
        return loggedInUser.getId();
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse updateUserByAdmin(String userId, AdminUserRequest request) {
        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        if (StringUtils.hasText(request.getEmail()) && !existingUser.getEmail().equals(request.getEmail())) {
            userRepository.findByEmail(request.getEmail()).ifPresent(userWithNewEmail -> {
                if (!userWithNewEmail.getId().equals(existingUser.getId())) {
                    throw new IllegalStateException("Email " + request.getEmail() + " is already in use by another account.");
                }
            });
            existingUser.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getName())) {
            existingUser.setName(request.getName());
        }

        if (StringUtils.hasText(request.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (StringUtils.hasText(request.getRole())) {
            existingUser.setRole(request.getRole());
        }

        UserEntity updatedUser = userRepository.save(existingUser);
        return convertToResponse(updatedUser);
    }


    @Override
    public UserResponse updateUserProfile(UserProfileUpdateRequest request) {
        String currentEmail = authenticationFacade.getAuthentication().getName();
        UserEntity loggedInUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found. Please log in again."));

        if (StringUtils.hasText(request.getName())) {
            loggedInUser.setName(request.getName());
        }
        if (StringUtils.hasText(request.getPassword())) {
            loggedInUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }


        if (StringUtils.hasText(request.getEmail()) && !loggedInUser.getEmail().equals(request.getEmail())) {
            if(userRepository.findByEmail(request.getEmail()).isPresent()){
                throw new IllegalStateException("Email " + request.getEmail() + " is already in use by another account.");
            }
            loggedInUser.setEmail(request.getEmail());
        }

        UserEntity updatedUser = userRepository.save(loggedInUser);
        return convertToResponse(updatedUser);
    }
    @Override
    public UserResponse getUserById(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        return convertToResponse(user);
    }

    @Override
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // --- (Helper methods remain the same) ---
    private UserEntity convertToEntity(UserRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role("ROLE_USER")
                .build();
    }

    private UserEntity convertAdminUserToEntity(AdminUserRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .build();
    }

    private UserResponse convertToResponse(UserEntity registeredUser) {
        return UserResponse.builder()
                .id(registeredUser.getId())
                .name(registeredUser.getName())
                .email(registeredUser.getEmail())
                .role(registeredUser.getRole())
                .build();
    }
}