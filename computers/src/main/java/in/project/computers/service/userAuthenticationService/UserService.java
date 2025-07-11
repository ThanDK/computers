package in.project.computers.service.userAuthenticationService;

import in.project.computers.dto.user.UserRequest;
import in.project.computers.dto.user.UserResponse;

public interface UserService {
    UserResponse registerUser(UserRequest request);

    String findByUserId();
}
