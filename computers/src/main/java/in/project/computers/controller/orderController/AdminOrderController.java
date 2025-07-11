package in.project.computers.controller.orderController;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.dto.order.ShipOrderRequest;
import in.project.computers.service.orderService.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List; // Import List

/**
 * <h3>Admin Order Controller (Final Version)</h3>
 * <p>
 * Controller สำหรับจัดการ API Endpoints ทั้งหมดที่เกี่ยวกับการจัดการ Order ซึ่งต้องใช้สิทธิ์ของผู้ดูแลระบบ (Admin)
 * </p>
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * <h4>[GET] /api/admin/orders</h4>
     * <p>Endpoint สำหรับ Admin เพื่อดึงรายการ Order ทั้งหมดในระบบ</p>
     * <p><b>การทำงาน:</b> จะดึง Order ทั้งหมดจากฐานข้อมูลและส่งกลับไปเป็น JSON Array</p>
     * @return ResponseEntity ที่มี List ของ OrderResponse
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Admin action: Fetching all orders.");
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * <h4>[GET] /api/admin/orders/{orderId}</h4>
     * <p>Endpoint สำหรับ Admin เพื่อดูรายละเอียด Order ใดๆ ก็ได้ในระบบ</p>
     * @param orderId ID ของ Order ที่ต้องการดูรายละเอียด
     * @return ResponseEntity ที่มีรายละเอียดทั้งหมดของ Order
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getAnyOrderById(@PathVariable String orderId) {
        log.info("Admin action: Fetching order details for ID: {}", orderId);
        OrderResponse response = orderService.getAnyOrderByIdForAdmin(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * <h4>[POST] /api/admin/orders/approve-slip/{orderId}</h4>
     * <p>Endpoint สำหรับ Admin เพื่ออนุมัติสลิปโอนเงินที่ผู้ใช้ส่งมา</p>
     * @param orderId ID ของ Order ที่จะอนุมัติสลิป
     * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะอัปเดตเป็น PROCESSING
     */
    @PostMapping("/approve-slip/{orderId}")
    public ResponseEntity<OrderResponse> approvePaymentSlip(@PathVariable String orderId) {
        log.info("Admin action: Approving payment slip for order ID: {}", orderId);
        OrderResponse response = orderService.approvePaymentSlip(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * <h4>[POST] /api/admin/orders/ship/{orderId}</h4>
     * <p>Endpoint สำหรับ Admin เพื่ออัปเดตข้อมูลการจัดส่ง</p>
     * @param orderId ID ของ Order ที่จะจัดส่ง
     * @param request DTO ที่มีข้อมูล shippingProvider และ trackingNumber
     * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะอัปเดตเป็น SHIPPED
     */
    @PostMapping("/ship/{orderId}")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Shipping order ID: {}", orderId);
        OrderResponse response = orderService.shipOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * <h4>[POST] /api/admin/orders/approve-refund/{orderId}</h4>
     * <p>Endpoint สำหรับ Admin เพื่ออนุมัติคำขอคืนเงิน</p>
     * @param orderId ID ของ Order ที่จะอนุมัติการคืนเงิน
     * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะอัปเดตเป็น REFUNDED
     */
    @PostMapping("/approve-refund/{orderId}")
    public ResponseEntity<OrderResponse> approveRefund(@PathVariable String orderId) {
        try {
            log.info("Admin action: Approving refund for order ID: {}", orderId);
            OrderResponse response = orderService.approveRefund(orderId);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            log.error("Admin action: Error processing PayPal refund for order ID: {}. Error: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing PayPal refund: " + e.getMessage(), e);
        }
    }

    /**
     * <h4>[POST] /api/admin/orders/reject-refund/{orderId}</h4>
     * <p>Endpoint สำหรับ Admin เพื่อปฏิเสธคำขอคืนเงิน</p>
     * @param orderId ID ของ Order ที่จะปฏิเสธการคืนเงิน
     * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะอัปเดตเป็น REFUND_REJECTED
     */
    @PostMapping("/reject-refund/{orderId}")
    public ResponseEntity<OrderResponse> rejectRefund(@PathVariable String orderId) {
        log.info("Admin action: Rejecting refund for order ID: {}", orderId);
        OrderResponse response = orderService.rejectRefund(orderId);
        return ResponseEntity.ok(response);
    }
}