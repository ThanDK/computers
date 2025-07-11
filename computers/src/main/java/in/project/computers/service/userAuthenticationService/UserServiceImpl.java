package in.project.computers.service.userAuthenticationService;

import in.project.computers.dto.user.UserRequest;
import in.project.computers.dto.user.UserResponse;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalRepo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service สำหรับจัดการตรรกะที่เกี่ยวข้องกับผู้ใช้ เช่น การลงทะเบียน
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationFacade authenticationFacade;

    /**
     * ลงทะเบียนผู้ใช้ใหม่ในระบบ
     * @param request ข้อมูลผู้ใช้ใหม่
     * @return UserResponse DTO ของผู้ใช้ที่ลงทะเบียนแล้ว
     */
    @Override
    public UserResponse registerUser(UserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("อีเมล " + request.getEmail() + " นี้มีผู้ใช้งานแล้ว");
        }
        UserEntity newUser = convertToEntity(request);
        newUser = userRepository.save(newUser);
        return convertToResponse(newUser);
    }

    /**
     * ค้นหา ID ของผู้ใช้ที่กำลังล็อกอินอยู่
     * @return ID ของผู้ใช้ (String)
     */
    @Override
    public String findByUserId() {
        String loggedInUserEmail = authenticationFacade.getAuthentication().getName();
        UserEntity loggedInUser = userRepository.findByEmail(loggedInUserEmail)
                .orElseThrow(() -> new UsernameNotFoundException("ไม่พบผู้ใช้"));
        return loggedInUser.getId();
    }

    /**
     * [Helper] แปลง UserRequest DTO เป็น UserEntity
     */
    private UserEntity convertToEntity(UserRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role("ROLE_USER") // กำหนด role เริ่มต้นเป็น USER
                .build();
    }

    /**
     * [Helper] แปลง UserEntity เป็น UserResponse DTO
     */
    private UserResponse convertToResponse(UserEntity registeredUser) {
        return UserResponse.builder()
                .id(registeredUser.getId())
                .name(registeredUser.getName())
                .email(registeredUser.getEmail())
                .role(registeredUser.getRole())
                .build();
    }
}