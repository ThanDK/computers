package in.project.computers.DTO.order.orderResponse;

import in.project.computers.DTO.address.AddressDTO;
import in.project.computers.entity.order.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private String id;
    private String userId;
    private AddressDTO shippingAddress;
    private String email;
    private List<OrderLineItem> lineItems;
    private BigDecimal totalAmount;
    private String currency;
    private BigDecimal taxAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentDetailsResponse paymentDetails;
    private ShippingDetails shippingDetails;
    private Instant createdAt;
    private Instant updatedAt;
}