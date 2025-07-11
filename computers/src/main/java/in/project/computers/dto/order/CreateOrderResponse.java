package in.project.computers.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderResponse {

    private String orderId;

    private String paypalOrderId;
    private String approvalLink;

    public CreateOrderResponse(String orderId) {
        this.orderId = orderId;
    }
}