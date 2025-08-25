package in.project.computers.controller.orderController;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.CreateOrderResponse;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.service.orderService.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * สร้างคำสั่งซื้อใหม่จากตะกร้าสินค้าของผู้ใช้
     * <p>
     * Endpoint นี้เป็นจุดเริ่มต้นของกระบวนการสั่งซื้อทั้งหมด หากผู้ใช้เลือกชำระเงินผ่าน PayPal,
     * ระบบจะสร้างลิงก์สำหรับชำระเงินและส่งกลับไปใน Response หากเลือกการโอนเงิน,
     * ระบบจะสร้างคำสั่งซื้อในสถานะ "รอการชำระเงิน"
     * </p>
     * @param request อ็อบเจกต์ {@link CreateOrderRequest} ที่มีข้อมูลสำหรับสร้างคำสั่งซื้อ เช่น ที่อยู่และวิธีการชำระเงิน
     * @return ResponseEntity ที่มีข้อมูล {@link CreateOrderResponse} และลิงก์ชำระเงิน (หากเป็น PayPal)
     * @throws ResponseStatusException หากเกิดข้อผิดพลาดในการสื่อสารกับ PayPal
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            log.info("User authenticated, received request to create order from their saved cart.");
            CreateOrderResponse response = orderService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            log.error("Error communicating with PayPal during order creation. Error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with PayPal", e);
        }
    }

    /**
     * อัปโหลดหลักฐานการชำระเงิน (สลิป)
     * <p>
     * Endpoint นี้ใช้สำหรับคำสั่งซื้อที่เลือกชำระเงินโดยการโอนเงิน ผู้ใช้จะส่งไฟล์รูปภาพสลิปมาเพื่อยืนยันการชำระเงิน
     * </p>
     * @param orderId ID ของคำสั่งซื้อ (จาก Path Variable)
     * @param slipImage ไฟล์รูปภาพสลิป (จาก Multipart Form Data)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตสถานะเป็น "รอการตรวจสอบ"
     */
    @PostMapping(value = "/submit-slip/{orderId}", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> submitSlip(
            @PathVariable String orderId,
            @RequestPart("slipImage") MultipartFile slipImage) {
        log.info("User authenticated, received payment slip for order ID: {}", orderId);
        OrderResponse response = orderService.submitPaymentSlip(orderId, slipImage);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint สำหรับยืนยันการชำระเงินผ่าน PayPal สำเร็จ (Callback)
     * <p>
     * URL นี้จะถูกเรียกโดย PayPal หลังจากผู้ใช้ชำระเงินเรียบร้อยแล้ว ระบบจะทำการยืนยันการชำระเงิน,
     * ตัดสต็อก, และอัปเดตสถานะคำสั่งซื้อ จากนั้นจะ Redirect ผู้ใช้กลับไปยังหน้า Frontend
     * </p>
     * @param orderId ID ของคำสั่งซื้อ (จาก Path Variable)
     * @param paymentId ID การชำระเงินจาก PayPal (จาก Query Parameter)
     * @param payerId ID ของผู้ชำระเงินจาก PayPal (จาก Query Parameter)
     * @return {@link RedirectView} ไปยังหน้า 'payment-successful' หรือ 'payment-failed' ของ Frontend
     */
    @GetMapping("/capture/{orderId}")
    public RedirectView captureOrder(
            @PathVariable String orderId,
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId) {
        try {
            log.info("Capturing PayPal payment for order ID: {}, Payment ID: {}", orderId, paymentId);
            orderService.capturePaypalOrder(orderId, paymentId, payerId);
            String redirectUrl = frontendUrl + "/payment-successful?order_id=" + orderId;
            log.info("Redirecting to success URL: {}", redirectUrl);
            return new RedirectView(redirectUrl);
        } catch (PayPalRESTException e) {
            log.error("Error capturing payment with PayPal for order ID: {}. Error: {}", orderId, e.getMessage());
            String redirectUrl = frontendUrl + "/payment-failed?order_id=" + orderId + "&error=capture_error";
            log.info("Redirecting to failure URL: {}", redirectUrl);
            return new RedirectView(redirectUrl);
        }
    }

    /**
     * Endpoint เมื่อผู้ใช้ยกเลิกการชำระเงินบนหน้า PayPal (Callback)
     * <p>
     * URL นี้จะถูกเรียกโดย PayPal หากผู้ใช้กดยกเลิกบนหน้าชำระเงิน ระบบจะ Redirect ผู้ใช้กลับไปยังหน้า Frontend
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่ถูกยกเลิก (จาก Path Variable)
     * @return {@link RedirectView} ไปยังหน้า 'payment-cancelled' ของ Frontend
     */
    @GetMapping("/cancel/{orderId}")
    public RedirectView paymentCancelled(@PathVariable String orderId) {
        log.warn("User cancelled PayPal payment for order ID: {}.", orderId);
        String redirectUrl = frontendUrl + "/payment-cancelled?order_id=" + orderId;
        log.info("Redirecting to cancellation URL: {}", redirectUrl);
        return new RedirectView(redirectUrl);
    }

    /**
     * ดึงข้อมูลคำสั่งซื้อทั้งหมดของผู้ใช้ที่ล็อกอินอยู่
     * <p>
     * Endpoint นี้สำหรับให้ผู้ใช้ดูประวัติการสั่งซื้อของตนเอง
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link OrderResponse}
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponse>> getUserOrders() {
        log.info("User authenticated, fetching their orders.");
        return ResponseEntity.ok(orderService.getCurrentUserOrders());
    }

    /**
     * ดึงข้อมูลคำสั่งซื้อตาม ID ที่ระบุ
     * <p>
     * Endpoint นี้สำหรับให้ผู้ใช้ดูรายละเอียดของคำสั่งซื้อรายการใดรายการหนึ่ง โดยระบบจะตรวจสอบความเป็นเจ้าของ
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่ต้องการ (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ของคำสั่งซื้อ
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {
        log.info("User authenticated, fetching order details for ID: {}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * ยกเลิกคำสั่งซื้อโดยผู้ใช้
     * <p>
     * Endpoint นี้อนุญาตให้ผู้ใช้ยกเลิกคำสั่งซื้อของตนเองได้ หากคำสั่งซื้อนั้นยังอยู่ในสถานะที่สามารถยกเลิกได้ (เช่น ยังไม่ได้ชำระเงิน)
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่ต้องการยกเลิก (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตสถานะเป็น CANCELLED
     */
    @PostMapping("/cancel-by-user/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancelOrderByUser(@PathVariable String orderId) {
        log.info("User authenticated, requesting to cancel order ID: {}", orderId);
        OrderResponse response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * สร้างลิงก์ชำระเงิน PayPal ใหม่อีกครั้ง
     * <p>
     * Endpoint นี้ใช้สำหรับคำสั่งซื้อที่การชำระเงินครั้งก่อนล้มเหลว หรือผู้ใช้ปิดหน้าต่างไปก่อนชำระเงินสำเร็จ
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่ต้องการลองชำระเงินใหม่ (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link CreateOrderResponse} และลิงก์ชำระเงิน PayPal ใหม่
     * @throws ResponseStatusException หากเกิดข้อผิดพลาดในการสร้างลิงก์ใหม่กับ PayPal
     */
    @PostMapping("/retry-paypal/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateOrderResponse> retryPaypalPayment(@PathVariable String orderId) {
        try {
            log.info("User authenticated, requesting to retry PayPal payment for order ID: {}", orderId);
            CreateOrderResponse response = orderService.retryPayment(orderId);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            log.error("Error creating new PayPal payment link for order ID {}. Error: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating new PayPal payment link", e);
        }
    }

    /**
     * ส่งคำร้องขอคืนเงินสำหรับคำสั่งซื้อ
     * <p>
     * Endpoint นี้จะเปลี่ยนสถานะของคำสั่งซื้อเป็น "รอการอนุมัติคืนเงิน" (REFUND_REQUESTED) เพื่อให้ผู้ดูแลระบบตรวจสอบต่อไป
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่ต้องการขอคืนเงิน (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตสถานะแล้ว
     */
    @PostMapping("/request-refund/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> requestRefund(@PathVariable String orderId) {
        log.info("User authenticated, requesting refund for order ID: {}", orderId);
        OrderResponse response = orderService.requestRefund(orderId);
        return ResponseEntity.ok(response);
    }
}