package in.project.computers.DTO.user.userRequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileUpdateRequest {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Pattern(regexp = "^[\\p{L}0-9 ]+$", message = "Name contains invalid characters")
    private String name;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email is too long")
    private String email;

    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String password;
}