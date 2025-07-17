package in.project.computers.dto.order;

import in.project.computers.entity.order.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO สำหรับแสดงรายละเอียดของ Order ทั้งหมด
 */
@Data
@Builder
public class OrderResponse {
    private String id;
    private String userId;
    private String userAddress;
    private String phoneNumber;
    private String email;
    private List<OrderLineItem> lineItems;
    private BigDecimal totalAmount;
    private String currency;
    private BigDecimal taxAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentDetails paymentDetails;
    private ShippingDetails shippingDetails;
    private Instant createdAt;
    private Instant updatedAt;
}