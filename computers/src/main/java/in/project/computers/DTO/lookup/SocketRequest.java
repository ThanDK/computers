package in.project.computers.DTO.lookup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocketRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;
    @NotBlank(message = "Brand cannot be blank")
    private String brand;
}