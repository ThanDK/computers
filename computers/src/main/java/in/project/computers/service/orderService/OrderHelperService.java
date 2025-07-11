package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.PaymentDetails;
import in.project.computers.entity.order.PaymentMethod;
import in.project.computers.entity.user.UserEntity;

/**
 * <h3>Order Helper Service Interface (ฉบับสมบูรณ์)</h3>
 * <p>
 * Interface นี้กำหนด "สัญญา" สำหรับคลาสผู้ช่วย (Helper) ที่จะเข้ามาจัดการ
 * Logic ที่ซับซ้อนในการประมวลผล Order
 * </p>
 * <p>
 * <b>การเปลี่ยนแปลงสำคัญ:</b>
 * <ul>
 *   <li>เมธอด <code>unpackAndAggregateItems</code> ถูกลบออกไป เนื่องจากเป็น Logic ภายในที่ไม่ควรเป็นส่วนหนึ่งของ Public Interface</li>
 *   <li>เมธอด <code>createAndValidateBaseOrder</code> ถูกปรับ Signature ใหม่ให้รับแค่ <code>CreateOrderRequest</code> และ <code>UserEntity</code> ซึ่งสะท้อนการทำงานของ Implementation ใหม่ที่เรียบง่ายขึ้น</li>
 * </ul>
 * </p>
 */
public interface OrderHelperService {

    Order createAndValidateBaseOrder(CreateOrderRequest request, UserEntity currentUser);

    void decrementStockForOrder(Order order);

    void incrementStockForOrder(Order order);

    void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException;

    String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog);

    Order findOrderForProcessing(String orderId, String userId, PaymentMethod expectedMethod);

    OrderResponse entityToResponse(Order order);
}