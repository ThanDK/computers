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

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * ดึงรายการคำสั่งซื้อทั้งหมดในระบบ
     * <p>
     * Endpoint นี้สำหรับผู้ดูแลระบบเพื่อดูภาพรวมของคำสั่งซื้อทั้งหมดที่มีในระบบ
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link OrderResponse} และสถานะ 200 OK
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Admin action: Fetching all orders.");
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * ดึงข้อมูลคำสั่งซื้อตาม ID ที่ระบุ
     * <p>
     * Endpoint นี้อนุญาตให้ผู้ดูแลระบบเข้าถึงรายละเอียดของคำสั่งซื้อใดๆ ก็ได้โดยไม่ต้องตรวจสอบความเป็นเจ้าของ
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่ต้องการดึงข้อมูล (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ของคำสั่งซื้อและสถานะ 200 OK
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getAnyOrderById(@PathVariable String orderId) {
        log.info("Admin action: Fetching order details for ID: {}", orderId);
        OrderResponse response = orderService.getAnyOrderByIdForAdmin(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * อนุมัติหลักฐานการชำระเงิน (สลิป)
     * <p>
     * Endpoint นี้ใช้สำหรับยืนยันการชำระเงินผ่านการโอนเงินที่ผู้ใช้ส่งมา ระบบจะตัดสต็อกสินค้าและเปลี่ยนสถานะคำสั่งซื้อเป็น "กำลังดำเนินการ" (PROCESSING)
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่จะอนุมัติสลิป (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PostMapping("/approve-slip/{orderId}")
    public ResponseEntity<OrderResponse> approvePaymentSlip(@PathVariable String orderId) {
        log.info("Admin action: Approving payment slip for order ID: {}", orderId);
        OrderResponse response = orderService.approvePaymentSlip(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * บันทึกข้อมูลการจัดส่งและอัปเดตสถานะคำสั่งซื้อ
     * <p>
     * Endpoint นี้ใช้สำหรับบันทึกข้อมูลการจัดส่ง เช่น บริษัทขนส่งและหมายเลขพัสดุ จากนั้นจะเปลี่ยนสถานะคำสั่งซื้อเป็น "จัดส่งแล้ว" (SHIPPED)
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่จะจัดส่ง (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link ShipOrderRequest} ที่มีข้อมูลการจัดส่ง
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PostMapping("/ship/{orderId}")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Shipping order ID: {}", orderId);
        OrderResponse response = orderService.shipOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * อนุมัติคำขอคืนเงิน
     * <p>
     * Endpoint นี้จะเริ่มกระบวนการคืนเงิน หากเป็นการชำระผ่าน PayPal ระบบจะเรียก API เพื่อคืนเงินโดยอัตโนมัติ จากนั้นจะคืนสต็อกสินค้าและเปลี่ยนสถานะเป็น REFUNDED
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่จะคืนเงิน (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
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
     * บังคับคืนเงินสำหรับคำสั่งซื้อ
     * <p>
     * Endpoint นี้ใช้ในกรณีพิเศษที่ผู้ดูแลระบบต้องการคืนเงินให้ผู้ใช้โดยไม่ต้องมีคำขอ เช่น ตรวจพบข้อผิดพลาดของสินค้า
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่จะคืนเงิน (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
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
     * ปฏิเสธคำขอคืนเงิน
     * <p>
     * Endpoint นี้จะเปลี่ยนสถานะคำสั่งซื้อเป็น REFUND_REJECTED เพื่อแจ้งให้ผู้ใช้ทราบว่าคำขอถูกปฏิเสธ
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่จะปฏิเสธ (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PostMapping("/reject-refund/{orderId}")
    public ResponseEntity<OrderResponse> rejectRefund(@PathVariable String orderId) {
        log.info("Admin action: Rejecting refund for order ID: {}", orderId);
        OrderResponse response = orderService.rejectRefund(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * ปฏิเสธหลักฐานการชำระเงิน (สลิป)
     * <p>
     * Endpoint นี้ใช้เมื่อสลิปที่ผู้ใช้ส่งมาไม่ถูกต้อง ระบบจะเปลี่ยนสถานะเพื่อให้ผู้ใช้อัปโหลดใหม่
     * </p>
     * @param orderId ID ของคำสั่งซื้อ (จาก Path Variable)
     * @param payload JSON object ที่ต้องมี key ชื่อ "reason" พร้อมค่าที่เป็น String สำหรับบอกเหตุผล
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
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
     * ย้อนกลับการอนุมัติสลิปที่เคยอนุมัติไปแล้ว
     * <p>
     * Endpoint นี้ใช้ในกรณีที่ผู้ดูแลระบบกดอนุมัติสลิปผิดพลาด ระบบจะคืนสต็อกสินค้าและเปลี่ยนสถานะกลับไปรอการตรวจสอบใหม่
     * </p>
     * @param orderId ID ของคำสั่งซื้อ (จาก Path Variable)
     * @param payload JSON object ที่ต้องมี key ชื่อ "reason" พร้อมค่าที่เป็น String สำหรับบอกเหตุผล
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
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
     * แก้ไขข้อมูลการจัดส่งของคำสั่งซื้อที่ส่งไปแล้ว
     * <p>
     * Endpoint นี้ใช้สำหรับแก้ไขข้อมูลการจัดส่งในกรณีที่กรอกผิดพลาด
     * </p>
     * @param orderId ID ของคำสั่งซื้อที่ต้องการแก้ไข (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link ShipOrderRequest} ที่มีข้อมูลการจัดส่งใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/update-shipping/{orderId}")
    public ResponseEntity<OrderResponse> updateShippingDetails(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Updating shipping details for order ID: {}", orderId);
        OrderResponse response = orderService.updateShippingDetails(orderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * เปลี่ยนสถานะของคำสั่งซื้อด้วยตนเอง
     * <p>
     * Endpoint นี้อนุญาตให้ผู้ดูแลระบบเปลี่ยนสถานะของคำสั่งซื้อไปยังสถานะใดๆ ก็ได้ตามที่ระบุ
     * </p>
     * @param orderId ID ของคำสั่งซื้อ (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link UpdateOrderStatusRequest} ที่มีสถานะใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link OrderResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PostMapping("/status/{orderId}")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable String orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Admin action: Manually updating status for order ID {} to {}", orderId, request.getNewStatus());
        OrderResponse response = orderService.updateOrderStatus(orderId, request.getNewStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * ดึงรายการสถานะถัดไปที่คำสั่งซื้อสามารถเปลี่ยนไปได้
     * <p>
     * Endpoint นี้มีไว้สำหรับช่วยในส่วนของ Frontend เพื่อแสดงตัวเลือกสถานะที่ถูกต้องตามลำดับขั้นตอน
     * </p>
     * @param orderId ID ของคำสั่งซื้อ (จาก Path Variable)
     * @return ResponseEntity ที่มี List ของ {@link OrderStatus} ที่เป็นไปได้และสถานะ 200 OK
     */
    @GetMapping("/next-statuses/{orderId}")
    public ResponseEntity<List<OrderStatus>> getValidNextStatuses(@PathVariable String orderId) {
        List<OrderStatus> statuses = orderService.getValidNextStatuses(orderId);
        return ResponseEntity.ok(statuses);
    }

    /**
     * ดึงรายการสถานะคำสั่งซื้อทั้งหมดที่เป็นไปได้
     * <p>
     * Endpoint นี้มีไว้สำหรับดึงค่า Enum ทั้งหมดของ {@link OrderStatus} เพื่อใช้ในส่วนของ Frontend เช่น การสร้างตัวกรอง (Filter) หรือ Dropdown
     * </p>
     * @return ResponseEntity ที่มี List ของชื่อสถานะทั้งหมดและสถานะ 200 OK
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getAllOrderStatuses() {
        return ResponseEntity.ok(Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.toList()));
    }
}