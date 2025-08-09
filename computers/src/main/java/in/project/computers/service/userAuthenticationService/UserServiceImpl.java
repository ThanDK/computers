package in.project.computers.service.userAuthenticationService;

import in.project.computers.DTO.user.userRequest.AdminUserRequest;
import in.project.computers.DTO.user.userRequest.UserProfileUpdateRequest;
import in.project.computers.DTO.user.userRequest.UserRequest;
import in.project.computers.DTO.user.userResponse.UserResponse;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalReposiroty.UserRepository;
import in.project.computers.service.awsS3Bucket.S3Service;
import in.project.computers.service.userAuthenticationService.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus; // <-- IMPORTED
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException; // <-- IMPORTED

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationFacade authenticationFacade;
    private final S3Service s3Service;
    private final UserMapper userMapper;

    @Override
    public UserResponse registerUser(UserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email " + request.getEmail() + " already exists");
        }
        UserEntity newUser = userMapper.convertToEntity(request);
        newUser = userRepository.save(newUser);
        return userMapper.convertToResponse(newUser);
    }

    @Override
    public UserResponse AdminCreateUser(AdminUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email " + request.getEmail() + " already exists");
        }
        UserEntity newUser = userMapper.convertAdminUserToEntity(request);
        newUser = userRepository.save(newUser);
        return userMapper.convertToResponse(newUser);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));
        return userMapper.convertToResponse(user);
    }

    @Override
    public String findByUserId() {
        String loggedInUserEmail = authenticationFacade.getAuthentication().getName();
        UserEntity loggedInUser = userRepository.findByEmail(loggedInUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found with email: " + loggedInUserEmail));
        return loggedInUser.getId();
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse updateUserByAdmin(String userId, AdminUserRequest request) {
        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

        if (StringUtils.hasText(request.getEmail()) && !existingUser.getEmail().equals(request.getEmail())) {
            userRepository.findByEmail(request.getEmail()).ifPresent(userWithNewEmail -> {
                if (!userWithNewEmail.getId().equals(existingUser.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email " + request.getEmail() + " is already in use by another account.");
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
        return userMapper.convertToResponse(updatedUser);
    }

    @Override
    public UserResponse updateUserProfile(UserProfileUpdateRequest request, MultipartFile file) {
        String currentEmail = authenticationFacade.getAuthentication().getName();
        UserEntity loggedInUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Please log in again."));

        if (StringUtils.hasText(request.getName())) {
            loggedInUser.setName(request.getName());
        }
        if (StringUtils.hasText(request.getPassword())) {
            loggedInUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (StringUtils.hasText(request.getEmail()) && !loggedInUser.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email " + request.getEmail() + " is already in use by another account.");
            }
            loggedInUser.setEmail(request.getEmail());
        }

        if (file != null && !file.isEmpty()) {
            if (StringUtils.hasText(loggedInUser.getProfilePictureUrl())) {
                String oldFileKey = s3Service.extractKeyFromUrl(loggedInUser.getProfilePictureUrl());
                if (oldFileKey != null) {
                    s3Service.deleteFileByKey(oldFileKey);
                }
            }
            String newProfilePictureUrl = s3Service.uploadFile(file);
            loggedInUser.setProfilePictureUrl(newProfilePictureUrl);
        }

        UserEntity updatedUser = userRepository.save(loggedInUser);
        return userMapper.convertToResponse(updatedUser);
    }

    @Override
    public UserResponse removeUserProfilePicture() {
        String currentEmail = authenticationFacade.getAuthentication().getName();
        UserEntity loggedInUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found. Cannot remove profile picture."));

        if (StringUtils.hasText(loggedInUser.getProfilePictureUrl())) {
            String fileKey = s3Service.extractKeyFromUrl(loggedInUser.getProfilePictureUrl());
            if (fileKey != null) {
                s3Service.deleteFileByKey(fileKey);
            }
            loggedInUser.setProfilePictureUrl(null);
            userRepository.save(loggedInUser);
        }

        return userMapper.convertToResponse(loggedInUser);
    }

    @Override
    public UserResponse getUserById(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));
        return userMapper.convertToResponse(user);
    }

    @Override
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public UserResponse lockUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));
        user.setLocked(true);
        UserEntity lockedUser = userRepository.save(user);
        log.info("Admin has LOCKED user account for ID: {}", userId);
        return userMapper.convertToResponse(lockedUser);
    }

    @Override
    public UserResponse unlockUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));
        user.setLocked(false);
        UserEntity unlockedUser = userRepository.save(user);
        log.info("Admin has UNLOCKED user account for ID: {}", userId);
        return userMapper.convertToResponse(unlockedUser);
    }
}