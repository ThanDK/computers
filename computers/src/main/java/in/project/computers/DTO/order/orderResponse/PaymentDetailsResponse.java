package in.project.computers.DTO.order.orderResponse;

import in.project.computers.entity.order.PaymentMethod;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentDetailsResponse {
    private PaymentMethod paymentMethod;
    private String transactionId;
    private String providerStatus;
    private String slipImageUrl;
    private String slipRejectionReason;
    private String payerId;
    private String payerEmail;
    private String refundSlipUrl;
}