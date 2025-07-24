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
 * <h3>Order Controller (for User)</h3>
 * <p>
 * Controller for handling user-facing order actions, such as creating an order from a cart,
 * submitting payment slips, viewing order history, and handling payment provider callbacks.
 * </p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * <h4>[POST] /api/orders</h4>
     * <p>Endpoint for creating a new order FROM the user's saved cart.</p>
     * <p><b>Workflow:</b> The backend reads the user's cart items from the `CartService` and creates an order.</p>
     * @param request DTO containing only checkout information (address, phone, payment method).
     * @return ResponseEntity with CreateOrderResponse (e.g., PayPal link or just an Order ID).
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
     * <h4>[POST] /api/orders/submit-slip/{orderId}</h4>
     * <p>Endpoint for submitting a payment slip for a BANK_TRANSFER order.</p>
     * @param orderId   The ID of the order.
     * @param slipImage The slip image file.
     * @return ResponseEntity with the updated order details.
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
     * <p>PayPal Success Callback. This endpoint is public.</p>
     * <p>Redirects the user to the frontend's payment success or failure page.</p>
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
     * <p>PayPal Cancel Callback. This endpoint is public.</p>
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
     * <p>Endpoint for a user to get their own order history.</p>
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponse>> getUserOrders() {
        log.info("User authenticated, fetching their orders.");
        return ResponseEntity.ok(orderService.getCurrentUserOrders());
    }

    /**
     * <h4>[GET] /api/orders/{orderId}</h4>
     * <p>Endpoint for a user to get details of a specific order they own.</p>
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {
        log.info("User authenticated, fetching order details for ID: {}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * <h4>[POST] /api/orders/cancel-by-user/{orderId}</h4>
     * <p>Endpoint for a user to cancel their own order if it's still in a pending state.</p>
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
     * <p>Endpoint for a user to retry a failed or pending PayPal payment.</p>
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
     * <p>Endpoint for a user to request a refund on a processed order.</p>
     */
    @PostMapping("/request-refund/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> requestRefund(@PathVariable String orderId) {
        log.info("User authenticated, requesting refund for order ID: {}", orderId);
        OrderResponse response = orderService.requestRefund(orderId);
        return ResponseEntity.ok(response);
    }
}