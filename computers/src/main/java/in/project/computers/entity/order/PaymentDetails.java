package in.project.computers.entity.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails {


    private PaymentMethod paymentMethod;

    private String transactionId;


    private String slipImageUrl;


    private String slipRejectionReason;


    private String providerStatus;

    private String payerId;


    private String payerEmail;
}