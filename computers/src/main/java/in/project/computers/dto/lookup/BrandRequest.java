package in.project.computers.dto.lookup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BrandRequest {
    @NotBlank(message = "Brand name cannot be blank")
    private String name;
}