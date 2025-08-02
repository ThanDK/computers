package in.project.computers.DTO.user.userResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AuthenticationResponse {
    private String email;
    private String token;
}
