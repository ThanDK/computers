package in.project.computers.entity.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "paymentMethods")
public class PaymentMethod {

    @Id
    private String id;

    private String bankName;

    private String accountName;

    private String accountNumber;

    private String qrCodeImageUrl;

    @Builder.Default
    private boolean isDefault = false;
}