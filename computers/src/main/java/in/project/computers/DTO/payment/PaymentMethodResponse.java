package in.project.computers.DTO.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodResponse {
    private String id;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String qrCodeImageUrl;
    @JsonProperty("isDefault")
    private boolean isDefault;
}

