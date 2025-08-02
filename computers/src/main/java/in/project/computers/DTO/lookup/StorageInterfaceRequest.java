package in.project.computers.DTO.lookup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorageInterfaceRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name; 
}