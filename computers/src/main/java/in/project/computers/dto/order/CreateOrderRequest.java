package in.project.computers.dto.order;

import in.project.computers.entity.order.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "User address is required")
    private String userAddress;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Payment method must be specified")
    private PaymentMethod paymentMethod;
}