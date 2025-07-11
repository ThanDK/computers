package in.project.computers.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO สำหรับรับข้อมูลการจัดส่งจาก Admin
 */
@Data
public class ShipOrderRequest {

    @NotBlank(message = "Shipping provider cannot be blank")
    private String shippingProvider;

    @NotBlank(message = "Tracking number cannot be blank")
    private String trackingNumber;
}