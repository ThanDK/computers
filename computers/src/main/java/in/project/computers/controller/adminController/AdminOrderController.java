package in.project.computers.controller.adminController;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.DTO.order.orderRequest.ShipOrderRequest;
import in.project.computers.DTO.order.orderRequest.UpdateOrderStatusRequest;
import in.project.computers.entity.order.OrderStatus;
import in.project.computers.service.orderService.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller สำหรับจัดการ Order ซึ่งต้องใช้สิทธิ์ Admin เท่านั้น
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * ดึงรายการ Order ทั้งหมดในระบบ
     * @return List ของ Order ทั้งหมด
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Admin action: Fetching all orders.");
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * ดูรายละเอียด Order ใดๆ ก็ได้ในระบบ
     * @param orderId ID ของ Order ที่ต้องการ
     * @return Order ที่มีรายละเอียดครบถ้วน
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getAnyOrderById(@PathVariable String orderId) {
        log.info("Admin action: Fetching order details for ID: {}", orderId);
        OrderResponse response = orderService.getAnyOrderByIdForAdmin(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * อนุมัติสลิปโอนเงินที่ผู้ใช้ส่งมา และเปลี่ยนสถานะเป็น PROCESSING
     * @param orderId ID ของ Order ที่จะอนุมัติ
     * @return Order ที่อัปเดตสถานะแล้ว
     */
    @PostMapping("/approve-slip/{orderId}")
    public ResponseEntity<OrderResponse> approvePaymentSlip(@PathVariable String orderId) {
        log.info("Admin action: Approving payment slip for order ID: {}", orderId);
        OrderResponse response = orderService.approvePaymentSlip(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * อัปเดตข้อมูลการจัดส่งของ Order และเปลี่ยนสถานะเป็น SHIPPED
     * @param orderId ID ของ Order ที่จะจัดส่ง
     * @param request ข้อมูลการจัดส่ง เช่น บริษัทขนส่งและ Tracking Number
     * @return Order ที่อัปเดตข้อมูลแล้ว
     */
    @PostMapping("/ship/{orderId}")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Shipping order ID: {}", orderId);
        OrderResponse response = orderService.shipOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * อนุมัติคำขอคืนเงิน (Trigger การคืนเงินผ่าน PayPal) และเปลี่ยนสถานะเป็น REFUNDED
     * @param orderId ID ของ Order ที่จะคืนเงิน
     * @return Order ที่อัปเดตสถานะแล้ว
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
     * บังคับคืนเงิน (Force Refund) โดยไม่สนเงื่อนไข และเปลี่ยนสถานะเป็น REFUNDED
     * @param orderId ID ของ Order ที่จะคืนเงิน
     * @return Order ที่อัปเดตสถานะแล้ว
     */
    @PostMapping("/force-refund/{orderId}")
    public ResponseEntity<OrderResponse> forceRefundByAdmin(@PathVariable String orderId) {
        try {
            log.info("Admin action: Forcing a refund for order ID: {}", orderId);
            OrderResponse response = orderService.forceRefundByAdmin(orderId);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            log.error("Admin action: Error processing forced PayPal refund for order ID: {}. Error: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing forced PayPal refund: " + e.getMessage(), e);
        }
    }

    /**
     * ปฏิเสธคำขอคืนเงิน และเปลี่ยนสถานะเป็น REFUND_REJECTED
     * @param orderId ID ของ Order ที่จะปฏิเสธ
     * @return Order ที่อัปเดตสถานะแล้ว
     */
    @PostMapping("/reject-refund/{orderId}")
    public ResponseEntity<OrderResponse> rejectRefund(@PathVariable String orderId) {
        log.info("Admin action: Rejecting refund for order ID: {}", orderId);
        OrderResponse response = orderService.rejectRefund(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * ปฏิเสธสลิปโอนเงิน และเปลี่ยนสถานะเป็น PAYMENT_REJECTED
     * @param orderId ID ของ Order
     * @param payload JSON object ที่มี key "reason" สำหรับบอกเหตุผล
     * @return Order ที่อัปเดตสถานะแล้ว
     */
    @PostMapping("/reject-slip/{orderId}")
    public ResponseEntity<OrderResponse> rejectPaymentSlip(@PathVariable String orderId, @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason for rejection is required.");
        }
        log.info("Admin action: Rejecting payment slip for order ID: {}", orderId);
        OrderResponse response = orderService.rejectPaymentSlip(orderId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * ย้อนกลับการอนุมัติสลิปที่เคยอนุมัติไปแล้ว และเปลี่ยนสถานะกลับเป็น PENDING_APPROVAL
     * @param orderId ID ของ Order
     * @param payload JSON object ที่มี key "reason" สำหรับบอกเหตุผล
     * @return Order ที่อัปเดตสถานะแล้ว
     */
    @PostMapping("/revert-approval/{orderId}")
    public ResponseEntity<OrderResponse> revertSlipApproval(@PathVariable String orderId, @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason for reversion is required.");
        }
        log.info("Admin action: Reverting slip approval for order ID: {}", orderId);
        OrderResponse response = orderService.revertSlipApproval(orderId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * แก้ไขข้อมูลการจัดส่งของ Order ที่จัดส่งไปแล้ว
     * @param orderId ID ของ Order ที่ต้องการแก้ไข
     * @param request ข้อมูลการจัดส่งใหม่
     * @return Order ที่อัปเดตข้อมูลแล้ว
     */
    @PutMapping("/update-shipping/{orderId}")
    public ResponseEntity<OrderResponse> updateShippingDetails(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Updating shipping details for order ID: {}", orderId);
        OrderResponse response = orderService.updateShippingDetails(orderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * เปลี่ยนสถานะของ Order ด้วยตนเอง (Manual Update)
     * @param orderId ID ของ Order
     * @param request ข้อมูลสถานะใหม่
     * @return Order ที่อัปเดตสถานะแล้ว
     */
    @PostMapping("/status/{orderId}")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable String orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Admin action: Manually updating status for order ID {} to {}", orderId, request.getNewStatus());
        OrderResponse response = orderService.updateOrderStatus(orderId, request.getNewStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * ดึงรายการสถานะถัดไปที่ Order สามารถเปลี่ยนไปได้ (สำหรับใช้ใน UI)
     * @param orderId ID ของ Order
     * @return List ของสถานะที่เป็นไปได้
     */
    @GetMapping("/next-statuses/{orderId}")
    public ResponseEntity<List<OrderStatus>> getValidNextStatuses(@PathVariable String orderId) {
        List<OrderStatus> statuses = orderService.getValidNextStatuses(orderId);
        return ResponseEntity.ok(statuses);
    }

    /**
     * ดึงรายการสถานะ Order ทั้งหมดที่มีในระบบ (สำหรับใช้ใน UI filter)
     * @return List ของชื่อสถานะทั้งหมด
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getAllOrderStatuses() {
        return ResponseEntity.ok(Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.toList()));
    }
}