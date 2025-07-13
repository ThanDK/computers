package in.project.computers.dto.lookup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RamTypeRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name; // e.g., "DDR4", "DDR5"
}