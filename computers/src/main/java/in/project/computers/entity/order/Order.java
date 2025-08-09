package in.project.computers.entity.order;

import in.project.computers.entity.user.Address;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "orders")
@Data
@Builder
public class Order {
    @Id
    private String id;

    // --- ส่วนข้อมูลผู้ใช้และที่อยู่ ---
    private String userId;
    private Address shippingAddress;
    private String email;

    // --- ส่วนข้อมูลรายการสินค้าและยอดรวม ---
    private List<OrderLineItem> lineItems;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;
    private String currency;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount;
    // --- ส่วนสถานะของ Order ---
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // --- ส่วนรายละเอียดการชำระเงิน ---
    private PaymentDetails paymentDetails;

    // --- ส่วนรายละเอียดการจัดส่ง ---
    private ShippingDetails shippingDetails;

    // --- ส่วนเวลา ---
    private Instant createdAt;
    private Instant updatedAt;
}