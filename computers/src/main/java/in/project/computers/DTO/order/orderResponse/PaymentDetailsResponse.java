package in.project.computers.DTO.order.orderResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.project.computers.entity.order.PaymentMethod;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDetailsResponse {
    private PaymentMethod paymentMethod;
    private String transactionId;
    private String providerStatus;

    private String slipImageUrl;
    private String slipRejectionReason;

    private String payerId;
    private String payerEmail;
}