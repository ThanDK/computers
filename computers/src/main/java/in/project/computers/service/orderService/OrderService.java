package in.project.computers.service.orderService;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.CreateOrderResponse;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.dto.order.ShipOrderRequest;
import in.project.computers.entity.order.OrderStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


public interface OrderService {

    /**
     * สร้างคำสั่งซื้อ (Order) ใหม่ตามข้อมูลที่ได้รับจาก Client
     * <p>
     * เมธอดนี้เป็นจุดเริ่มต้นของกระบวนการสั่งซื้อทั้งหมด จะรับผิดชอบตั้งแต่การตรวจสอบความถูกต้องของข้อมูล,
     * การรวบรวมรายการสินค้า, ตรวจสอบสต็อก, คำนวณยอดรวม, ไปจนถึงการเริ่มต้นกระบวนการชำระเงินตามวิธีที่ผู้ใช้เลือก
     * </p>
     *
     * @param request Data Transfer Object (DTO) ที่มีข้อมูลคำสั่งซื้อทั้งหมดจาก Client เช่น รายการสินค้า, ที่อยู่, วิธีชำระเงิน
     * @return {@link CreateOrderResponse} ซึ่งเป็น DTO ที่มีข้อมูลสำหรับ Client เพื่อดำเนินการต่อในขั้นตอนถัดไป
     *         เช่น มี Order ID ที่สร้างขึ้นใหม่ และอาจมีลิงก์สำหรับไปชำระเงินที่ PayPal (`approvalLink`) ในกรณีที่เลือกชำระเงินด้วยวิธีดังกล่าว
     * @throws PayPalRESTException หากเลือกชำระเงินด้วย PayPal แล้วเกิดข้อผิดพลาดในการติดต่อกับ PayPal API ขณะสร้างลิงก์ชำระเงิน
     */
    CreateOrderResponse createOrder(CreateOrderRequest request) throws PayPalRESTException;

    /**
     * ยืนยันและประมวลผลการชำระเงินผ่าน PayPal หลังจากที่ผู้ใช้ทำรายการสำเร็จบนหน้าเว็บของ PayPal
     * <p>
     * เมธอดนี้จะถูกเรียกโดย Endpoint ที่กำหนดไว้เป็น Callback URL กับ PayPal เมื่อผู้ใช้กดยินยอมการชำระเงิน
     * หน้าที่หลักคือการยืนยันการทำรายการกับ PayPal, ตัดสต็อกสินค้าในคลัง, และอัปเดตสถานะของ Order เป็น "ชำระเงินสำเร็จแล้ว"
     * </p>
     *
     * @param orderId   ID ของ Order ในระบบของเรา ซึ่งถูกส่งไป-กลับกับ PayPal เพื่อใช้อ้างอิง
     * @param paymentId ID ของการชำระเงิน (Payment) ที่สร้างโดย PayPal (ได้รับจาก Query Parameter ของ Callback URL)
     * @param payerId   ID ของผู้ชำระเงิน (Payer) ที่ระบุโดย PayPal (ได้รับจาก Query Parameter ของ Callback URL)
     * @return {@link OrderResponse} ที่มีสถานะของ Order ที่อัปเดตแล้ว (เช่น ชำระเงินสำเร็จแล้ว, กำลังดำเนินการ)
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการยืนยันการชำระเงินกับ PayPal API
     */
    OrderResponse capturePaypalOrder(String orderId, String paymentId, String payerId) throws PayPalRESTException;

    /**
     * รับไฟล์สลิปโอนเงินจากผู้ใช้สำหรับ Order ที่เลือกชำระเงินแบบ Bank Transfer
     * <p>
     * เมธอดนี้จะทำการอัปโหลดไฟล์รูปภาพสลิปไปยังบริการจัดเก็บไฟล์ (เช่น AWS S3)
     * จากนั้นจะบันทึก URL ของรูปภาพลงในฐานข้อมูล และอัปเดตสถานะของ Order เป็น "รอการตรวจสอบ" (Pending Approval)
     * </p>
     *
     * @param orderId   ID ของ Order ที่ต้องการแจ้งชำระเงิน
     * @param slipImage ไฟล์รูปภาพสลิปที่ผู้ใช้อัปโหลดมา
     * @return {@link OrderResponse} ที่มีสถานะของ Order ที่อัปเดตแล้ว
     */
    OrderResponse submitPaymentSlip(String orderId, MultipartFile slipImage);

    /**
     * ดึงข้อมูล Order ตาม ID ที่ระบุ
     * <p>
     * เมธอดนี้จะมีการตรวจสอบสิทธิ์เพื่อให้แน่ใจว่าผู้ใช้ที่ร้องขอข้อมูลเป็นเจ้าของ Order นั้นจริงๆ
     * เพื่อป้องกันการเข้าถึงข้อมูลของผู้อื่นโดยไม่ได้รับอนุญาต
     * </p>
     *
     * @param orderId ID ของ Order ที่ต้องการดูข้อมูล
     * @return {@link OrderResponse} ที่มีรายละเอียดทั้งหมดของ Order นั้น
     */
    OrderResponse getOrderById(String orderId);

    /**
     * ดึงรายการ Order ทั้งหมดของผู้ใช้ที่กำลังล็อกอินอยู่ในปัจจุบัน
     * <p>
     * ผลลัพธ์จะถูกเรียงลำดับตามวันที่สร้างล่าสุด (ออเดอร์ใหม่สุดจะแสดงอยู่บนสุด)
     * </p>
     *
     * @return {@code List<OrderResponse>} รายการ Order ทั้งหมดของผู้ใช้คนดังกล่าว
     */
    List<OrderResponse> getCurrentUserOrders();

    /**
     * [สำหรับ User] ยกเลิก Order ที่ยังอยู่ในสถานะที่อนุญาตให้ยกเลิกได้
     * <p>
     * โดยทั่วไปคือ Order ที่ยังไม่ได้ชำระเงิน (สถานะ PENDING_PAYMENT)
     * </p>
     *
     * @param orderId ID ของ Order ที่ต้องการยกเลิก
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น CANCELLED
     * @throws ResponseStatusException หาก Order ไม่อยู่ในสถานะที่สามารถยกเลิกได้ (เช่น จ่ายเงินไปแล้ว หรือถูกยกเลิกไปแล้ว)
     */
    OrderResponse cancelOrder(String orderId);

    /**
     * [สำหรับ User] พยายามชำระเงินใหม่อีกครั้งสำหรับ Order ที่เลือกชำระด้วย PayPal
     * <p>
     * ใช้ในกรณีที่การชำระเงินครั้งแรกล้มเหลว หรือผู้ใช้ปิดหน้าต่างไปก่อนที่จะชำระเงินสำเร็จ
     * ระบบจะสร้างลิงก์สำหรับไปชำระเงินที่ PayPal ใหม่อีกครั้ง
     * </p>
     *
     * @param orderId ID ของ Order ที่ต้องการลองชำระเงินใหม่
     * @return {@link CreateOrderResponse} ที่มีลิงก์สำหรับไปชำระเงินที่ PayPal ใหม่ (`approvalLink`)
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการสร้างลิงก์กับ PayPal API
     * @throws ResponseStatusException หาก Order ไม่ตรงตามเงื่อนไข (เช่น ไม่ใช่ Order ของ PayPal หรือชำระเงินไปแล้ว)
     */
    CreateOrderResponse retryPayment(String orderId) throws PayPalRESTException;

    /**
     * [สำหรับ User] ส่งคำขอคืนเงินสำหรับ Order ที่ได้ชำระเงินและจัดส่งไปแล้ว (หรืออยู่ในสถานะที่อนุญาตให้คืนเงินได้)
     * <p>
     * ระบบจะเปลี่ยนสถานะ Order เป็น REFUND_REQUESTED เพื่อให้ Admin เข้ามาตรวจสอบและดำเนินการในขั้นตอนต่อไป
     * </p>
     *
     * @param orderId ID ของ Order ที่ต้องการขอคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUND_REQUESTED
     * @throws ResponseStatusException หาก Order ไม่อยู่ในสถานะที่สามารถขอคืนเงินได้
     */
    OrderResponse requestRefund(String orderId);

    /**
     * [สำหรับ Admin] อนุมัติคำขอคืนเงินที่ผู้ใช้ส่งเข้ามา
     * <p>
     * หากเป็นการชำระเงินผ่าน PayPal ระบบจะพยายามดำเนินการคืนเงินผ่าน PayPal API โดยอัตโนมัติ
     * หากเป็น Bank Transfer จะเป็นการอนุมัติในระบบเพื่อให้ Admin ไปโอนเงินคืนเอง
     * จากนั้นจะทำการคืนสต็อกสินค้าเข้าระบบ และอัปเดตสถานะ Order เป็น REFUNDED
     * </p>
     *
     * @param orderId ID ของ Order ที่ Admin ต้องการอนุมัติการคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUNDED
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อ PayPal API เพื่อทำการ Refund
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่รอการอนุมัติคืนเงิน
     */
    OrderResponse approveRefund(String orderId) throws PayPalRESTException;

    /**
     * [สำหรับ Admin] อัปเดตข้อมูลการจัดส่งและเปลี่ยนสถานะ Order เป็น SHIPPED
     * <p>
     * เมธอดนี้จะถูกเรียกโดย Admin เพื่อกรอกข้อมูลบริษัทขนส่งและหมายเลขพัสดุ
     * ระบบจะตรวจสอบว่า Order อยู่ในสถานะที่พร้อมจัดส่ง (เช่น PROCESSING) หรือไม่
     * จากนั้นจะบันทึกข้อมูลการจัดส่งและเปลี่ยนสถานะ Order เป็น "จัดส่งแล้ว"
     * </p>
     *
     * @param orderId ID ของ Order ที่ต้องการจัดส่ง
     * @param request DTO ที่มีข้อมูล shippingProvider และ trackingNumber
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น SHIPPED และมีข้อมูลการจัดส่ง
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่พร้อมจัดส่ง
     */
    OrderResponse shipOrder(String orderId, ShipOrderRequest request);

    /**
     * [สำหรับ Admin] ปฏิเสธคำขอคืนเงินที่ผู้ใช้ส่งมา
     * <p>
     * ระบบจะเปลี่ยนสถานะ Order เป็น REFUND_REJECTED เพื่อแจ้งให้ผู้ใช้ทราบว่าคำขอถูกปฏิเสธ
     * </p>
     *
     * @param orderId ID ของ Order ที่ Admin ต้องการปฏิเสธการคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUND_REJECTED
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่รอการอนุมัติคืนเงิน
     */
    OrderResponse rejectRefund(String orderId);

    /**
     * [สำหรับ Admin] บังคับคืนเงิน Order โดยไม่ต้องรอคำขอจากผู้ใช้
     * <p>
     * ใช้ในสถานการณ์ที่ Admin ต้องการเริ่มกระบวนการคืนเงินเอง เช่น พบข้อบกพร่องของสินค้า,
     * ราคาผิดพลาด, หรือลูกค้าติดต่อขอคืนเงินผ่านช่องทางอื่น
     * จะทำงานกับ Order ที่ชำระเงินแล้ว (เช่น PROCESSING, SHIPPED)
     * </p>
     *
     * @param orderId ID ของ Order ที่ต้องการบังคับคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUNDED
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อ PayPal API เพื่อทำการ Refund
     * @throws ResponseStatusException หาก Order ไม่อยู่ในสถานะที่สามารถบังคับคืนเงินได้
     */
    OrderResponse forceRefundByAdmin(String orderId) throws PayPalRESTException;

    /**
     * [สำหรับ Admin] ดึงรายการ Order ทั้งหมดในระบบ
     * @return List<OrderResponse> ที่มีข้อมูล Order ทั้งหมด
     */
    List<OrderResponse> getAllOrders();

    /**
     * [สำหรับ Admin] อนุมัติสลิปโอนเงินที่ผู้ใช้ส่งมา
     * <p>
     * ระบบจะตรวจสอบว่า Order อยู่ในสถานะ "รอการตรวจสอบ" (Pending Approval) หรือไม่
     * จากนั้นจะทำการตัดสต็อกสินค้า และอัปเดตสถานะ Order เป็น "กำลังดำเนินการ" (PROCESSING)
     * </p>
     *
     * @param orderId ID ของ Order ที่จะอนุมัติสลิป
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น PROCESSING
     * @throws ResponseStatusException หาก Order ไม่ได้อยู่ในสถานะที่ถูกต้อง
     */
    OrderResponse approvePaymentSlip(String orderId);

    /**
     * [สำหรับ Admin] ดึงข้อมูล Order ใดๆ ก็ได้ในระบบโดยไม่ต้องตรวจสอบความเป็นเจ้าของ
     *
     * @param orderId ID ของ Order ที่ต้องการดูข้อมูล
     * @return {@link OrderResponse} ที่มีรายละเอียดทั้งหมดของ Order นั้น
     */
    OrderResponse getAnyOrderByIdForAdmin(String orderId);

    /**
     * [สำหรับ Admin] แก้ไขข้อมูลการจัดส่งสำหรับ Order ที่จัดส่งไปแล้ว
     *
     * @param orderId ID ของ Order ที่ต้องการแก้ไข
     * @param request DTO ที่มีข้อมูล shippingProvider และ trackingNumber ใหม่
     * @return {@link OrderResponse} ที่มีข้อมูลการจัดส่งที่อัปเดตแล้ว
     */
    OrderResponse updateShippingDetails(String orderId, ShipOrderRequest request);

    /**
     * [สำหรับ Admin] เปลี่ยนสถานะของ Order ไปยังสถานะถัดไปที่ถูกต้องด้วยตนเอง
     *
     * @param orderId ID ของ Order ที่ต้องการเปลี่ยนสถานะ
     * @param newStatus สถานะใหม่ที่ต้องการจะเปลี่ยนไป
     * @return {@link OrderResponse} ที่มีสถานะใหม่
     */
    OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus);

    /**
     * [สำหรับ Admin] ดึงรายการสถานะที่เป็นไปได้ถัดไปสำหรับ Order ที่กำหนด
     *
     * @param orderId ID ของ Order ที่ต้องการตรวจสอบ
     * @return List ของ OrderStatus ที่สามารถเปลี่ยนไปได้
     */
    List<OrderStatus> getValidNextStatuses(String orderId);
    /**
     * [สำหรับ Admin] ปฏิเสธสลิปโอนเงินที่ผู้ใช้ส่งมา
     *
     * @param orderId ID ของ Order ที่จะปฏิเสธสลิป
     * @param reason  เหตุผลที่ปฏิเสธ (สำหรับบันทึก)
     * @return {@link OrderResponse} ที่มีสถานะอัปเดตเป็น PENDING_PAYMENT และ REJECTED
     */
    OrderResponse rejectPaymentSlip(String orderId, String reason);

    /**
     * [สำหรับ Admin] ย้อนกลับการอนุมัติสลิปที่เคยอนุมัติไปแล้ว (สำหรับ Bank Transfer เท่านั้น)
     * <p>
     * ใช้ในกรณีที่ Admin กดอนุมัติผิดพลาด ระบบจะทำการคืนสต็อกสินค้า และเปลี่ยนสถานะกลับไปรอการชำระเงินใหม่
     * </p>
     *
     * @param orderId ID ของ Order ที่จะย้อนกลับ
     * @param reason  เหตุผลที่ย้อนกลับ (สำหรับบันทึก)
     * @return {@link OrderResponse} ที่มีสถานะอัปเดตกลับไปเป็น PENDING_PAYMENT
     */
    OrderResponse revertSlipApproval(String orderId, String reason);
}