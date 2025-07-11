package in.project.computers.dto.order;

import in.project.computers.entity.order.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {


    private Map<String, Integer> componentItems;

    private Map<String, Integer> buildItems;


    @NotBlank(message = "User address is required")
    private String userAddress;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;


    @NotNull(message = "Payment method must be specified")
    private PaymentMethod paymentMethod;
}