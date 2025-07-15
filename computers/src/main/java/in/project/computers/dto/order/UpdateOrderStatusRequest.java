package in.project.computers.dto.order;

import in.project.computers.entity.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    @NotNull(message = "New status cannot be null")
    private OrderStatus newStatus;
}