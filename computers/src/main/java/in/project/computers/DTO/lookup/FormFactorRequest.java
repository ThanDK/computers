package in.project.computers.DTO.lookup;

import in.project.computers.entity.lookup.FormFactorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FormFactorRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;
    @NotNull(message = "Type cannot be null")
    private FormFactorType type;
}