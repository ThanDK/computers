package in.project.computers.entity.order;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * <h3>Order Entity (ฉบับสมบูรณ์)</h3>
 * <p>
 * อ็อบเจกต์หลักที่ใช้แทนคำสั่งซื้อหนึ่งรายการในระบบ ถูกจัดเก็บใน Collection ชื่อ "orders"
 * </p>
 * <p>
 * <b>การเปลี่ยนแปลงล่าสุด:</b>
 * <ul>
 *   <li>เพิ่ม Field ใหม่: <code>shippingDetails</code> โดยอ้างอิงจากคลาส {@link ShippingDetails} ที่แยกไฟล์ออกไป</li>
 * </ul>
 * </p>
 */
@Document(collection = "orders")
@Data
@Builder
public class Order {
    @Id
    private String id;

    // --- ส่วนข้อมูลผู้ใช้และที่อยู่ ---
    private String userId;
    private String userAddress;
    private String phoneNumber;
    private String email;

    // --- ส่วนข้อมูลรายการสินค้าและยอดรวม ---
    private List<OrderLineItem> lineItems;
    private BigDecimal totalAmount;
    private String currency;

    // --- ส่วนสถานะของ Order ---
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // --- ส่วนรายละเอียดการชำระเงิน ---
    private PaymentDetails paymentDetails;

    // --- [ปรับปรุง] ส่วนรายละเอียดการจัดส่ง ---
    private ShippingDetails shippingDetails;

    // --- ส่วนเวลา ---
    private Instant createdAt;
    private Instant updatedAt;
}