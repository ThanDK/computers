package in.project.computers.DTO.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetStockRequest {
    @NotBlank(message = "Component ID cannot be blank.")
    private String componentId;

    @Min(value = 0, message = "Quantity cannot be negative.")
    private int quantity;
}