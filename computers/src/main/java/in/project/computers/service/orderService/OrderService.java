package in.project.computers.service.orderService;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.CreateOrderResponse;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.DTO.order.orderRequest.ShipOrderRequest;
import in.project.computers.entity.order.OrderStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public interface OrderService {

    /**
     * สร้างคำสั่งซื้อ (Order) ใหม่ตามข้อมูลที่ได้รับจาก Client
     * <p>
     * เมธอดนี้เป็นจุดเริ่มต้นของกระบวนการสั่งซื้อทั้งหมด จะรับผิดชอบตั้งแต่การตรวจสอบความถูกต้องของข้อมูล,
     * การรวบรวมรายการสินค้าจากตะกร้า, ตรวจสอบสต็อก, คำนวณยอดรวม, ไปจนถึงการเริ่มต้นกระบวนการชำระเงินตามวิธีที่ผู้ใช้เลือก
     * </p>
     * @param request DTO ที่มีข้อมูลคำสั่งซื้อทั้งหมดจาก Client เช่น ที่อยู่, วิธีชำระเงิน
     * @return {@link CreateOrderResponse} ซึ่งมี Order ID ที่สร้างขึ้นใหม่ และอาจมีลิงก์สำหรับไปชำระเงินที่ PayPal (`approvalLink`)
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อกับ PayPal API ขณะสร้างลิงก์ชำระเงิน
     */
    CreateOrderResponse createOrder(CreateOrderRequest request) throws PayPalRESTException;

    /**
     * ยืนยันและประมวลผลการชำระเงินผ่าน PayPal หลังจากที่ผู้ใช้ทำรายการสำเร็จ
     * <p>
     * เมธอดนี้จะถูกเรียกโดย Callback URL จาก PayPal เมื่อผู้ใช้กดยินยอมการชำระเงิน
     * หน้าที่หลักคือการยืนยันการทำรายการกับ PayPal, ตัดสต็อกสินค้า, และอัปเดตสถานะของ Order เป็น PROCESSING
     * </p>
     * @param orderId   ID ของ Order ในระบบของเราเพื่อใช้อ้างอิง
     * @param paymentId ID ของการชำระเงินที่สร้างโดย PayPal
     * @param payerId   ID ของผู้ชำระเงินที่ระบุโดย PayPal
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการยืนยันการชำระเงินกับ PayPal API
     */
    void capturePaypalOrder(String orderId, String paymentId, String payerId) throws PayPalRESTException;

    /**
     * รับไฟล์สลิปโอนเงินสำหรับ Order ที่เลือกชำระเงินแบบ Bank Transfer
     * <p>
     * เมธอดนี้จะทำการอัปโหลดไฟล์รูปภาพสลิป, บันทึก URL, และอัปเดตสถานะของ Order เป็น PENDING_APPROVAL
     * </p>
     * @param orderId   ID ของ Order ที่ต้องการแจ้งชำระเงิน
     * @param slipImage ไฟล์รูปภาพสลิปที่ผู้ใช้อัปโหลดมา
     * @return {@link OrderResponse} ที่มีสถานะของ Order ที่อัปเดตแล้ว
     */
    OrderResponse submitPaymentSlip(String orderId, MultipartFile slipImage);

    /**
     * ดึงข้อมูล Order ตาม ID ที่ระบุ โดยมีการตรวจสอบความเป็นเจ้าของ
     * <p>
     * เมธอดนี้จะตรวจสอบสิทธิ์เพื่อให้แน่ใจว่าผู้ใช้ที่ร้องขอข้อมูลเป็นเจ้าของ Order นั้นจริงๆ
     * </p>
     * @param orderId ID ของ Order ที่ต้องการดูข้อมูล
     * @return {@link OrderResponse} ที่มีรายละเอียดทั้งหมดของ Order นั้น
     * @throws ResponseStatusException หากไม่พบ Order หรือผู้ใช้ไม่มีสิทธิ์เข้าถึง
     */
    OrderResponse getOrderById(String orderId);

    /**
     * ดึงรายการ Order ทั้งหมดของผู้ใช้ที่กำลังล็อกอินอยู่ในปัจจุบัน
     * <p>
     * ผลลัพธ์จะถูกเรียงลำดับตามวันที่สร้างล่าสุด
     * </p>
     * @return {@code List<OrderResponse>} รายการ Order ทั้งหมดของผู้ใช้
     */
    List<OrderResponse> getCurrentUserOrders();

    /**
     * ยกเลิก Order ที่ยังอยู่ในสถานะที่อนุญาตให้ยกเลิกได้
     * <p>
     * โดยทั่วไปคือ Order ที่ยังไม่ได้ชำระเงิน (สถานะ PENDING_PAYMENT)
     * </p>
     * @param orderId ID ของ Order ที่ต้องการยกเลิก
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น CANCELLED
     * @throws ResponseStatusException หาก Order ไม่อยู่ในสถานะที่สามารถยกเลิกได้
     */
    OrderResponse cancelOrder(String orderId);

    /**
     * พยายามชำระเงินใหม่อีกครั้งสำหรับ Order ที่เลือกชำระด้วย PayPal
     * <p>
     * ใช้ในกรณีที่การชำระเงินครั้งแรกล้มเหลว ระบบจะสร้างลิงก์สำหรับไปชำระเงินที่ PayPal ใหม่อีกครั้ง
     * </p>
     * @param orderId ID ของ Order ที่ต้องการลองชำระเงินใหม่
     * @return {@link CreateOrderResponse} ที่มีลิงก์สำหรับไปชำระเงินที่ PayPal ใหม่ (`approvalLink`)
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการสร้างลิงก์กับ PayPal API
     * @throws ResponseStatusException หาก Order ไม่ตรงตามเงื่อนไข
     */
    CreateOrderResponse retryPayment(String orderId) throws PayPalRESTException;

    /**
     * ส่งคำขอคืนเงินสำหรับ Order ที่ได้ชำระเงินไปแล้ว
     * <p>
     * ระบบจะเปลี่ยนสถานะ Order เป็น REFUND_REQUESTED เพื่อให้ Admin เข้ามาตรวจสอบ
     * </p>
     * @param orderId ID ของ Order ที่ต้องการขอคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUND_REQUESTED
     * @throws ResponseStatusException หาก Order ไม่อยู่ในสถานะที่สามารถขอคืนเงินได้
     */
    OrderResponse requestRefund(String orderId);

    /**
     * อนุมัติคำขอคืนเงินที่ผู้ใช้ส่งเข้ามา
     * <p>
     * หากเป็นการชำระเงินผ่าน PayPal ระบบจะดำเนินการคืนเงินผ่าน API โดยอัตโนมัติ
     * จากนั้นจะทำการคืนสต็อกสินค้าเข้าระบบ และอัปเดตสถานะ Order เป็น REFUNDED
     * </p>
     * @param orderId ID ของ Order ที่ต้องการอนุมัติการคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUNDED
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อ PayPal API
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่รอการอนุมัติคืนเงิน
     */
    OrderResponse approveRefund(String orderId) throws PayPalRESTException;

    /**
     * อัปเดตข้อมูลการจัดส่งและเปลี่ยนสถานะ Order เป็น SHIPPED
     * <p>
     * เมธอดนี้จะบันทึกข้อมูลบริษัทขนส่งและหมายเลขพัสดุ และเปลี่ยนสถานะ Order เป็น "จัดส่งแล้ว"
     * </p>
     * @param orderId ID ของ Order ที่ต้องการจัดส่ง
     * @param request DTO ที่มีข้อมูล shippingProvider และ trackingNumber
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น SHIPPED
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่พร้อมจัดส่ง
     */
    OrderResponse shipOrder(String orderId, ShipOrderRequest request);

    /**
     * ปฏิเสธคำขอคืนเงินที่ผู้ใช้ส่งมา
     * <p>
     * ระบบจะเปลี่ยนสถานะ Order เป็น REFUND_REJECTED เพื่อแจ้งให้ผู้ใช้ทราบ
     * </p>
     * @param orderId ID ของ Order ที่ต้องการปฏิเสธการคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUND_REJECTED
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่รอการอนุมัติคืนเงิน
     */
    OrderResponse rejectRefund(String orderId);

    /**
     * บังคับคืนเงิน Order โดยไม่ต้องรอคำขอจากผู้ใช้
     * <p>
     * ใช้ในสถานการณ์ที่ Admin ต้องการเริ่มกระบวนการคืนเงินเอง เช่น พบข้อบกพร่องของสินค้า
     * </p>
     * @param orderId ID ของ Order ที่ต้องการบังคับคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUNDED
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อ PayPal API
     * @throws ResponseStatusException หาก Order ไม่อยู่ในสถานะที่สามารถบังคับคืนเงินได้
     */
    OrderResponse forceRefundByAdmin(String orderId) throws PayPalRESTException;

    /**
     * ดึงรายการ Order ทั้งหมดในระบบโดยไม่มีการกรอง
     * @return {@code List<OrderResponse>} ที่มีข้อมูล Order ทั้งหมด
     */
    List<OrderResponse> getAllOrders();

    /**
     * อนุมัติสลิปโอนเงินที่ผู้ใช้ส่งมา
     * <p>
     * ระบบจะตรวจสอบสถานะ, ตัดสต็อกสินค้า, และอัปเดตสถานะ Order เป็น PROCESSING
     * </p>
     * @param orderId ID ของ Order ที่จะอนุมัติสลิป
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น PROCESSING
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่ถูกต้อง
     */
    OrderResponse approvePaymentSlip(String orderId);

    /**
     * ดึงข้อมูล Order ใดๆ ก็ได้ในระบบโดยไม่ต้องตรวจสอบความเป็นเจ้าของ
     *
     * @param orderId ID ของ Order ที่ต้องการดูข้อมูล
     * @return {@link OrderResponse} ที่มีรายละเอียดทั้งหมดของ Order นั้น
     * @throws ResponseStatusException หากไม่พบ Order
     */
    OrderResponse getAnyOrderByIdForAdmin(String orderId);

    /**
     * แก้ไขข้อมูลการจัดส่งสำหรับ Order ที่จัดส่งไปแล้ว
     *
     * @param orderId ID ของ Order ที่ต้องการแก้ไข
     * @param request DTO ที่มีข้อมูล shippingProvider และ trackingNumber ใหม่
     * @return {@link OrderResponse} ที่มีข้อมูลการจัดส่งที่อัปเดตแล้ว
     * @throws ResponseStatusException หากไม่พบ Order หรือ Order ยังไม่ถูกจัดส่ง
     */
    OrderResponse updateShippingDetails(String orderId, ShipOrderRequest request);

    /**
     * เปลี่ยนสถานะของ Order ไปยังสถานะถัดไปที่ถูกต้องด้วยตนเอง
     *
     * @param orderId ID ของ Order ที่ต้องการเปลี่ยนสถานะ
     * @param newStatus สถานะใหม่ที่ต้องการจะเปลี่ยนไป
     * @return {@link OrderResponse} ที่มีสถานะใหม่
     * @throws ResponseStatusException หากการเปลี่ยนสถานะไม่ถูกต้องตาม Flow ที่กำหนด
     */
    OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus);

    /**
     * ดึงรายการสถานะที่เป็นไปได้ถัดไปสำหรับ Order ที่กำหนด
     *
     * @param orderId ID ของ Order ที่ต้องการตรวจสอบ
     * @return {@code List<OrderStatus>} ที่สามารถเปลี่ยนไปได้
     * @throws ResponseStatusException หากไม่พบ Order
     */
    List<OrderStatus> getValidNextStatuses(String orderId);

    /**
     * ปฏิเสธสลิปโอนเงินที่ผู้ใช้ส่งมา
     * <p>
     * ระบบจะเปลี่ยนสถานะ Order กลับไปให้ผู้ใช้สามารถอัปโหลดสลิปใหม่ได้
     * </p>
     * @param orderId ID ของ Order ที่จะปฏิเสธสลิป
     * @param reason  เหตุผลที่ปฏิเสธ
     * @return {@link OrderResponse} ที่มีสถานะอัปเดตเป็น REJECTED_SLIP
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่รอการตรวจสอบ
     */
    OrderResponse rejectPaymentSlip(String orderId, String reason);

    /**
     * ย้อนกลับการอนุมัติสลิปที่เคยอนุมัติไปแล้ว (สำหรับ Bank Transfer เท่านั้น)
     * <p>
     * ใช้ในกรณีที่ Admin กดอนุมัติผิดพลาด ระบบจะทำการคืนสต็อกสินค้า และเปลี่ยนสถานะกลับไปรอการชำระเงินใหม่
     * </p>
     * @param orderId ID ของ Order ที่จะย้อนกลับ
     * @param reason  เหตุผลที่ย้อนกลับ
     * @return {@link OrderResponse} ที่มีสถานะอัปเดตกลับไปเป็น REJECTED_SLIP
     * @throws ResponseStatusException หาก Order ไม่อยู่ในสถานะที่ถูกต้องสำหรับการย้อนกลับ
     */
    OrderResponse revertSlipApproval(String orderId, String reason);
}