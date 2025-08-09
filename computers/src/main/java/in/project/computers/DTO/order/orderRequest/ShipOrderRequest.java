package in.project.computers.DTO.order.orderRequest;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShipOrderRequest {

    @NotBlank(message = "Shipping provider cannot be blank")
    private String shippingProvider;

    @NotBlank(message = "Tracking number cannot be blank")
    private String trackingNumber;
}