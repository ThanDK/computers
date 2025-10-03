package in.project.computers.DTO.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentMethodRequest {

    @NotBlank(message = "Bank name is required.")
    private String bankName;

    @NotBlank(message = "Account name is required.")
    private String accountName;

    @NotBlank(message = "Account number is required.")
    private String accountNumber;
}