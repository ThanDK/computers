package in.project.computers.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileUpdateRequest {
    private String name;
    private String email;
    private String password;
}