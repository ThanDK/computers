package in.project.computers.controller.orderController;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.CreateOrderResponse;
import in.project.computers.dto.order.OrderResponse;
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

/**
 * <h3>Order Controller (สำหรับ User)</h3>
 * <p>
 * Controller สำหรับจัดการ API Endpoints ทั้งหมดที่เกี่ยวกับระบบ Order ที่ผู้ใช้ทั่วไปสามารถทำได้
 * ทุก Endpoint ในนี้ (ยกเว้น Callback จาก PayPal) ต้องการการยืนยันตัวตน (Authentication)
 * </p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // ดึงค่า URL ของ Frontend จาก application.properties เพื่อใช้ในการ Redirect
    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * <h4>[POST] /api/orders</h4>
     * <p>Endpoint สำหรับสร้างคำสั่งซื้อใหม่</p>
     * <p><b>การทำงาน:</b> รับข้อมูลสินค้า, ที่อยู่, และวิธีการชำระเงินจากผู้ใช้ในรูปแบบ JSON และส่งต่อไปยัง OrderService เพื่อสร้าง Order</p>
     * <p><b>ตัวอย่างการเรียก (Body):</b></p>
     * <pre>{@code
     * {
     *   "shippingAddress": { ... },
     *   "items": [ { "productId": "cpu-123", "quantity": 1 }, { "productId": "gpu-456", "quantity": 1 } ],
     *   "paymentMethod": "PAYPAL"
     * }
     * }</pre>
     * <p><b>สิ่งที่ต้องมี:</b> ต้องมี Token ของผู้ใช้ที่ล็อกอินแล้วใน Header (`Authorization: Bearer <TOKEN>`)</p>
     *
     * @param request DTO ที่มีข้อมูลคำสั่งซื้อ
     * @return ResponseEntity ที่มี CreateOrderResponse (อาจมี PayPal link หรือแค่ Order ID)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()") // ผู้ใช้ต้องล็อกอินก่อน
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            log.info("User authenticated, received request to create order.");
            CreateOrderResponse response = orderService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            log.error("Error communicating with PayPal during order creation. Error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with PayPal", e);
        }
    }

    /**
     * <h4>[POST] /api/orders/submit-slip/{orderId}</h4>
     * <p>Endpoint สำหรับยืนยันการชำระเงินด้วยสลิป (สำหรับ Bank Transfer)</p>
     * <p><b>การทำงาน:</b> รับไฟล์รูปภาพสลิปโอนเงินสำหรับ Order ที่มีอยู่</p>
     * <p><b>หมายเหตุ:</b> Request นี้ต้องเป็นแบบ `multipart/form-data`</p>
     * <p><b>สิ่งที่ต้องมี:</b> Token ของผู้ใช้ที่ล็อกอินแล้วใน Header (`Authorization: Bearer <TOKEN>`)</p>
     *
     * @param orderId   ID ของ Order ที่ต้องการแจ้งชำระเงิน
     * @param slipImage ไฟล์รูปภาพสลิปที่แนบมากับ key "slipImage"
     * @return ResponseEntity ที่มี OrderResponse ที่อัปเดตสถานะแล้ว
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
     * <h4>[GET] /api/orders/capture/{orderId}</h4>
     * <p>Endpoint สำหรับ Callback จาก PayPal เมื่อชำระเงินสำเร็จ (Public Endpoint)</p>
     * <p><b>การทำงาน:</b> Endpoint นี้จะถูกเรียกโดย Browser ของผู้ใช้หลังจากที่ PayPal redirect กลับมา
     * ระบบจะทำการยืนยันการจ่ายเงินกับ PayPal และอัปเดตสถานะ Order จากนั้นจะ redirect ผู้ใช้ไปยังหน้า "ชำระเงินสำเร็จ" ของ Frontend</p>
     *
     * @param orderId   ID ของ Order
     * @param paymentId ID ของ Payment จาก PayPal (Query Param)
     * @param payerId   ID ของผู้จ่าย จาก PayPal (Query Param)
     * @return RedirectView ไปยังหน้า "Payment Successful" ของ Frontend
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
     * <h4>[GET] /api/orders/cancel/{orderId}</h4>
     * <p>Endpoint สำหรับ Callback จาก PayPal เมื่อผู้ใช้ยกเลิกการชำระเงิน (Public Endpoint)</p>
     * <p><b>การทำงาน:</b> Endpoint นี้จะถูกเรียกโดย Browser ของผู้ใช้หลังจากกดยกเลิกที่หน้า PayPal
     * ระบบจะ redirect ผู้ใช้ไปยังหน้า "ยกเลิกการชำระเงิน" ของ Frontend</p>
     *
     * @param orderId ID ของ Order ที่ถูกยกเลิก
     * @return RedirectView ไปยังหน้า "Payment Cancelled" ของ Frontend
     */
    @GetMapping("/cancel/{orderId}")
    public RedirectView paymentCancelled(@PathVariable String orderId) {
        log.warn("User cancelled PayPal payment for order ID: {}.", orderId);
        String redirectUrl = frontendUrl + "/payment-cancelled?order_id=" + orderId;
        log.info("Redirecting to cancellation URL: {}", redirectUrl);
        return new RedirectView(redirectUrl);
    }

    /**
     * <h4>[GET] /api/orders</h4>
     * <p>Endpoint สำหรับให้ผู้ใช้ดูรายการ Order ทั้งหมดของตนเอง</p>
     * <p><b>สิ่งที่ต้องมี:</b> Token ของผู้ใช้ที่ล็อกอินแล้วใน Header (`Authorization: Bearer <TOKEN>`)</p>
     *
     * @return รายการ Order ทั้งหมดของผู้ใช้ (List of OrderResponse)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponse>> getUserOrders() {
        log.info("User authenticated, fetching their orders.");
        return ResponseEntity.ok(orderService.getCurrentUserOrders());
    }

    /**
     * <h4>[GET] /api/orders/{orderId}</h4>
     * <p>Endpoint สำหรับให้ผู้ใช้ดูรายละเอียด Order เฉพาะเจาะจงของตนเอง</p>
     * <p><b>สิ่งที่ต้องมี:</b> Token ของผู้ใช้ที่ล็อกอินแล้วใน Header (`Authorization: Bearer <TOKEN>`)</p>
     *
     * @param orderId ID ของ Order ที่ต้องการดู
     * @return รายละเอียดของ Order นั้น (OrderResponse)
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {
        log.info("User authenticated, fetching order details for ID: {}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * <h4>[POST] /api/orders/cancel-by-user/{orderId}</h4>
     * <p>Endpoint สำหรับให้ผู้ใช้ยกเลิก Order ที่ยังไม่ได้ชำระเงิน</p>
     * <p><b>สิ่งที่ต้องมี:</b> Token ของผู้ใช้ที่ล็อกอินแล้วใน Header (`Authorization: Bearer <TOKEN>`)</p>
     *
     * @param orderId ID ของ Order ที่ต้องการยกเลิก
     * @return OrderResponse ที่มีสถานะเป็น CANCELLED
     */
    @PostMapping("/cancel-by-user/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancelOrderByUser(@PathVariable String orderId) {
        log.info("User authenticated, requesting to cancel order ID: {}", orderId);
        OrderResponse response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * <h4>[POST] /api/orders/retry-paypal/{orderId}</h4>
     * <p>Endpoint สำหรับให้ผู้ใช้พยายามชำระเงินผ่าน PayPal อีกครั้ง</p>
     * <p><b>การทำงาน:</b> สำหรับ Order ที่เคยเลือก PayPal แต่ยังไม่ได้จ่าย หรือจ่ายแล้วล้มเหลว ระบบจะสร้างลิงก์ PayPal ใหม่อีกครั้ง</p>
     * <p><b>สิ่งที่ต้องมี:</b> Token ของผู้ใช้ที่ล็อกอินแล้วใน Header (`Authorization: Bearer <TOKEN>`)</p>
     *
     * @param orderId ID ของ Order ที่ต้องการลองชำระเงินใหม่
     * @return CreateOrderResponse ที่มีลิงก์สำหรับไปชำระเงินที่ PayPal ใหม่
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
     * <h4>[POST] /api/orders/request-refund/{orderId}</h4>
     * <p>Endpoint สำหรับให้ผู้ใช้ส่งคำขอคืนเงิน (Request Refund)</p>
     * <p><b>การทำงาน:</b> ระบบจะเปลี่ยนสถานะ Order เป็น REFUND_REQUESTED เพื่อรอการตรวจสอบจาก Admin</p>
     * <p><b>สิ่งที่ต้องมี:</b> Token ของผู้ใช้ที่ล็อกอินแล้วใน Header (`Authorization: Bearer <TOKEN>`)</p>
     *
     * @param orderId ID ของ Order ที่ต้องการขอคืนเงิน
     * @return OrderResponse ที่มีสถานะเป็น REFUND_REQUESTED
     */
    @PostMapping("/request-refund/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> requestRefund(@PathVariable String orderId) {
        log.info("User authenticated, requesting refund for order ID: {}", orderId);
        OrderResponse response = orderService.requestRefund(orderId);
        return ResponseEntity.ok(response);
    }
}