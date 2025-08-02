package in.project.computers.DTO.user.userRequest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileUpdateRequest {
    private String name;
    private String email;
    private String password;
}