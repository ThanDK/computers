package in.project.computers.controller.adminController;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.dto.order.ShipOrderRequest;
import in.project.computers.dto.order.UpdateOrderStatusRequest;
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
    /**
     * [POST] /api/admin/orders/reject-slip/{orderId}
     * Endpoint for Admin to reject a payment slip, requiring a reason.
     * @param orderId ID of the order.
     * @param payload A JSON object containing a "reason" key. e.g., {"reason": "Image is blurry"}
     * @return The updated order response.
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
     * [POST] /api/admin/orders/revert-approval/{orderId}
     * Endpoint for Admin to revert a previously approved bank transfer.
     * @param orderId ID of the order.
     * @param payload A JSON object containing a "reason" key. e.g., {"reason": "Approved by mistake"}
     * @return The updated order response.
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
     * <h4>[PUT] /api/admin/orders/update-shipping/{orderId}</h4>
     * <p>Endpoint สำหรับ Admin เพื่อแก้ไขข้อมูลการจัดส่งของ Order ที่จัดส่งไปแล้ว</p>
     * <p><b>การทำงาน:</b> ใช้ในกรณีที่ต้องการแก้ไขชื่อบริษัทขนส่งหรือหมายเลขพัสดุ หลังจากที่ได้บันทึกไปครั้งแรกแล้ว</p>
     * @param orderId ID ของ Order ที่ต้องการแก้ไขข้อมูลการจัดส่ง
     * @param request DTO ที่มีข้อมูล shippingProvider และ trackingNumber ที่อัปเดตแล้ว
     * @return ResponseEntity ที่มี OrderResponse พร้อมข้อมูลการจัดส่งที่อัปเดตแล้ว
     */
    @PutMapping("/update-shipping/{orderId}")
    public ResponseEntity<OrderResponse> updateShippingDetails(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Updating shipping details for order ID: {}", orderId);
        OrderResponse response = orderService.updateShippingDetails(orderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * <h4>[POST] /api/admin/orders/status/{orderId}</h4>
     * <p>Endpoint สำหรับ Admin เพื่อเปลี่ยนสถานะของ Order ด้วยตนเอง (Manual Update)</p>
     * <p><b>การทำงาน:</b> ระบบจะตรวจสอบก่อนว่าการเปลี่ยนจากสถานะปัจจุบันไปยังสถานะใหม่นั้นได้รับอนุญาตหรือไม่ (ตาม Logic ใน Service)</p>
     * @param orderId ID ของ Order ที่ต้องการเปลี่ยนสถานะ
     * @param request DTO ที่มีสถานะใหม่ (`newStatus`)
     * @return ResponseEntity ที่มี OrderResponse พร้อมสถานะที่อัปเดตแล้ว
     */
    @PostMapping("/status/{orderId}")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable String orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Admin action: Manually updating status for order ID {} to {}", orderId, request.getNewStatus());
        OrderResponse response = orderService.updateOrderStatus(orderId, request.getNewStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * <h4>[GET] /api/admin/orders/next-statuses/{orderId}</h4>
     * <p>Endpoint สำหรับดึงรายการสถานะถัดไปที่ Order สามารถเปลี่ยนไปได้</p>
     * <p><b>การทำงาน:</b> เป็น Helper endpoint สำหรับ Frontend เพื่อใช้สร้าง Dropdown หรือตัวเลือกให้ Admin สามารถเปลี่ยนสถานะ Order ได้อย่างถูกต้อง</p>
     * @param orderId ID ของ Order ที่ต้องการตรวจสอบ
     * @return ResponseEntity ที่มี List ของ OrderStatus ที่เป็นไปได้
     */
    @GetMapping("/next-statuses/{orderId}")
    public ResponseEntity<List<OrderStatus>> getValidNextStatuses(@PathVariable String orderId) {
        List<OrderStatus> statuses = orderService.getValidNextStatuses(orderId);
        return ResponseEntity.ok(statuses);
    }

    /**
     * <h4>[GET] /api/admin/orders/statuses</h4>
     * <p>Endpoint สำหรับดึงรายการสถานะ Order ทั้งหมดที่มีในระบบ</p>
     * <p><b>การทำงาน:</b> เป็น Helper endpoint สำหรับ Frontend เพื่อใช้สร้างตัวเลือกในการกรอง (Filter) รายการ Order ตามสถานะ</p>
     * @return ResponseEntity ที่มี List ของชื่อสถานะทั้งหมด (String)
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getAllOrderStatuses() {
        return ResponseEntity.ok(Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.toList()));
    }
}