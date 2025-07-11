package in.project.computers.dto.component.componentRequest;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Quantity change cannot be null")
    private int quantity;
}
