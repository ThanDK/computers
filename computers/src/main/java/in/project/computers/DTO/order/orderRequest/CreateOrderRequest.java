package in.project.computers.DTO.order.orderRequest;

import in.project.computers.DTO.address.AddressDTO;
import in.project.computers.entity.order.PaymentMethod;
import jakarta.validation.Valid;
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

    private String savedAddressId;

    @Valid
    private AddressDTO newAddress;

    @NotNull(message = "Payment method must be specified")
    private PaymentMethod paymentMethod;
}