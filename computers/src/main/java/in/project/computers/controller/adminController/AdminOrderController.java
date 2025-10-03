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
import org.springframework.web.multipart.MultipartFile;
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

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Admin action: Fetching all orders.");
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getAnyOrderById(@PathVariable String orderId) {
        log.info("Admin action: Fetching order details for ID: {}", orderId);
        OrderResponse response = orderService.getAnyOrderByIdForAdmin(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve-slip/{orderId}")
    public ResponseEntity<OrderResponse> approvePaymentSlip(@PathVariable String orderId) {
        log.info("Admin action: Approving payment slip for order ID: {}", orderId);
        OrderResponse response = orderService.approvePaymentSlip(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ship/{orderId}")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Shipping order ID: {}", orderId);
        OrderResponse response = orderService.shipOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/approve-refund/{orderId}", consumes = "multipart/form-data")
    public ResponseEntity<OrderResponse> approveRefund(
            @PathVariable String orderId,
            @RequestPart(value = "refundSlip", required = false) MultipartFile refundSlip) {
        try {
            log.info("Admin action: Approving refund for order ID: {}", orderId);
            OrderResponse response = orderService.approveRefund(orderId, refundSlip);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            log.error("Admin action: Error processing PayPal refund for order ID: {}. Error: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing PayPal refund: " + e.getMessage(), e);
        }
    }

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

    @PostMapping("/reject-refund/{orderId}")
    public ResponseEntity<OrderResponse> rejectRefund(@PathVariable String orderId) {
        log.info("Admin action: Rejecting refund for order ID: {}", orderId);
        OrderResponse response = orderService.rejectRefund(orderId);
        return ResponseEntity.ok(response);
    }

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

    @PutMapping("/update-shipping/{orderId}")
    public ResponseEntity<OrderResponse> updateShippingDetails(@PathVariable String orderId, @Valid @RequestBody ShipOrderRequest request) {
        log.info("Admin action: Updating shipping details for order ID: {}", orderId);
        OrderResponse response = orderService.updateShippingDetails(orderId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/status/{orderId}")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable String orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Admin action: Manually updating status for order ID {} to {}", orderId, request.getNewStatus());
        OrderResponse response = orderService.updateOrderStatus(orderId, request.getNewStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/next-statuses/{orderId}")
    public ResponseEntity<List<OrderStatus>> getValidNextStatuses(@PathVariable String orderId) {
        List<OrderStatus> statuses = orderService.getValidNextStatuses(orderId);
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getAllOrderStatuses() {
        return ResponseEntity.ok(Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.toList()));
    }

    @PutMapping(value = "/update-refund-slip/{orderId}", consumes = "multipart/form-data")
    public ResponseEntity<OrderResponse> updateRefundSlip(
            @PathVariable String orderId,
            @RequestPart("newSlipImage") MultipartFile newSlipImage) {
        log.info("Admin action: Updating refund slip for order ID: {}", orderId);
        OrderResponse response = orderService.updateRefundSlip(orderId, newSlipImage);
        return ResponseEntity.ok(response);
    }
}