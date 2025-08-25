package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.entity.order.Cart;
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.PaymentDetails;
import in.project.computers.entity.user.UserEntity;
import org.springframework.web.server.ResponseStatusException;

public interface OrderHelperService {

    /**
     * สร้างและตรวจสอบความถูกต้องของอ็อบเจกต์ Order จาก Cart ของผู้ใช้
     * <p>
     * เมธอดนี้จะแปลงข้อมูลจาก Cart ให้เป็นอ็อบเจ็กต์ Order ที่สมบูรณ์
     * โดยมีการตรวจสอบสต็อกสินค้า, คำนวณยอดรวม และกำหนดสถานะเริ่มต้น
     * </p>
     * @param cart อ็อบเจกต์ Cart ที่มีรายการสินค้าทั้งหมดที่ผู้ใช้ต้องการสั่งซื้อ
     * @param request ข้อมูลเพิ่มเติมจากผู้ใช้ เช่น ที่อยู่สำหรับจัดส่ง และวิธีการชำระเงิน
     * @param currentUser ข้อมูลผู้ใช้ปัจจุบันที่กำลังทำการสั่งซื้อ
     * @return อ็อบเจ็กต์ {@link Order} ที่พร้อมสำหรับบันทึกลงฐานข้อมูล
     * @throws ResponseStatusException หากตะกร้าว่างเปล่า, สต็อกสินค้าไม่เพียงพอ, หรือข้อมูลที่อยู่ไม่ถูกต้อง
     */
    Order createAndValidateOrderFromCart(Cart cart, CreateOrderRequest request, UserEntity currentUser);

    /**
     * ลดจำนวนสต็อกสินค้าคงคลังตามรายการใน Order
     * <p>
     * เมธอดนี้จะถูกเรียกใช้หลังจากยืนยันการชำระเงินสำเร็จแล้ว เพื่อให้แน่ใจว่าสินค้าถูกสงวนไว้สำหรับออเดอร์นี้
     * </p>
     * @param order ออเดอร์ที่ต้องการตัดสต็อก
     */
    void decrementStockForOrder(Order order);

    /**
     * เพิ่มจำนวนสต็อกสินค้าคงคลังคืนตามรายการใน Order
     * <p>
     * เมธอดนี้จะถูกเรียกใช้เมื่อมีการคืนเงินหรือยกเลิกออเดอร์ที่เคยตัดสต็อกไปแล้ว
     * </p>
     * @param order ออเดอร์ที่ต้องการคืนสต็อก
     */
    void incrementStockForOrder(Order order);

    /**
     * ประมวลผลการคืนเงินผ่าน PayPal API
     * <p>
     * จะดึงข้อมูล Sale ID จากการชำระเงินเดิมและเรียกใช้ API ของ PayPal เพื่อทำการคืนเงิน
     * จากนั้นอัปเดตสถานะใน PaymentDetails ตามผลลัพธ์ที่ได้
     * </p>
     * @param order ออเดอร์ที่ต้องการคืนเงิน
     * @param paymentDetails รายละเอียดการชำระเงินเดิมที่มี Transaction ID ของ PayPal
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อกับ PayPal API
     */
    void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException;

    /**
     * ดึง Sale ID ออกจากอ็อบเจกต์ Payment ของ PayPal
     * <p>
     * Sale ID เป็นรหัสอ้างอิงของการขายที่จำเป็นสำหรับการเรียก API คืนเงิน
     * </p>
     * @param originalPaypalPayment อ็อบเจกต์ Payment ที่ได้จากการชำระเงินครั้งแรก
     * @param orderIdForLog ID ของออเดอร์สำหรับใช้ใน Log กรณีเกิดข้อผิดพลาด
     * @return Sale ID ที่เป็น String
     */
    String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog);

    /**
     * แปลงอ็อบเจกต์ Order (Entity) ไปเป็น OrderResponse (DTO)
     * <p>
     * เป็นเมธอดมาตรฐานสำหรับแปลงข้อมูล Entity ให้อยู่ในรูปแบบที่ปลอดภัย
     * และเหมาะสมสำหรับการส่งกลับไปให้ Client
     * </p>
     * @param order อ็อบเจกต์ Entity ที่ต้องการแปลง
     * @return อ็อบเจ็กต์ {@link OrderResponse}
     */
    OrderResponse entityToResponse(Order order);

}