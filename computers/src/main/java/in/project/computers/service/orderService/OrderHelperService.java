package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.entity.order.Cart;
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.PaymentDetails;
import in.project.computers.entity.user.UserEntity;


public interface OrderHelperService {

    /**
     * ลดจำนวนสต็อกสินค้าตามรายการใน Order
     * <p>
     * มักจะถูกเรียกใช้หลังจากยืนยันการชำระเงินสำเร็จแล้ว
     * </p>
     * @param order ออเดอร์ที่ต้องการตัดสต็อก
     */
    void decrementStockForOrder(Order order);

    /**
     * เพิ่มจำนวนสต็อกสินค้าคืนตามรายการใน Order
     * <p>
     * มักจะถูกเรียกใช้เมื่อมีการยกเลิกหรือคืนเงินออเดอร์ที่เคยชำระเงินไปแล้ว
     * </p>
     * @param order ออเดอร์ที่ต้องการคืนสต็อก
     */
    void incrementStockForOrder(Order order);

    /**
     * ประมวลผลการคืนเงินผ่าน PayPal API
     * <p>
     * จะดึงข้อมูลการชำระเงินเดิมและเรียกใช้ API เพื่อทำการคืนเงิน
     * </p>
     *
     * @param order          ออเดอร์ที่ต้องการคืนเงิน
     * @param paymentDetails รายละเอียดการชำระเงินเดิมที่มี Transaction ID ของ PayPal
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อกับ PayPal API
     */
    void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException;

    /**
     * ดึง Sale ID ออกจากอ็อบเจกต์ Payment ของ PayPal
     * <p>
     * Sale ID เป็นสิ่งจำเป็นสำหรับการเรียก API คืนเงิน (Refund)
     * </p>
     * @param originalPaypalPayment อ็อบเจกต์ Payment ที่ได้จากการชำระเงินครั้งแรก
     * @param orderIdForLog         ID ของออเดอร์สำหรับใช้ใน Log กรณีเกิดข้อผิดพลาด
     * @return Sale ID ที่เป็น String
     */
    String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog);

    /**
     * แปลงอ็อบเจกต์ Order (Entity) ไปเป็น OrderResponse (DTO)
     * @param order อ็อบเจกต์ Entity ที่ต้องการแปลง
     * @return อ็อบเจกต์ {@link OrderResponse} สำหรับส่งกลับไปให้ Client
     */
    OrderResponse entityToResponse(Order order);

    Order createAndValidateOrderFromCart(Cart cart, CreateOrderRequest request, UserEntity currentUser);
}