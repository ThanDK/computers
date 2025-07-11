package in.project.computers.service.orderService;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.CreateOrderResponse;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.dto.order.ShipOrderRequest;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalRepo.OrderRepository;
import in.project.computers.repository.generalRepo.UserRepository;
import in.project.computers.service.AWSS3Bucket.S3Service;
import in.project.computers.service.PaypalService.PaypalService;
import in.project.computers.service.userAuthenticationService.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

// คลาสหลักสำหรับจัดการ Business Logic ทั้งหมดที่เกี่ยวกับ Order
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    // --- Dependencies ---
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderHelperService orderHelper;
    private final UserService userService;
    private final S3Service s3Service;
    private final PaypalService paypalService;

    // --- Config Properties ---
    @Value("${paypal.payment.cancelUrl}")
    private String cancelUrl;
    @Value("${paypal.payment.successUrl}")
    private String successUrl;

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) throws PayPalRESTException {
        // === [CO-1] เริ่มกระบวนการสร้าง Order: ตรวจสอบและดึงข้อมูลผู้ใช้ปัจจุบัน ===
        UserEntity currentUser = userRepository.findById(userService.findByUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // === [CO-2] [ปรับปรุง] มอบหมายงานสร้าง Order ทั้งหมดให้ผู้ช่วย (OrderHelperService) ===
        // ส่งต่อ Request และข้อมูลผู้ใช้ทั้งหมดไปยัง OrderHelperService เพื่อจัดการตรรกะที่ซับซ้อนทั้งหมด
        // ตั้งแต่การตรวจสอบสต็อก, คำนวณราคา, ไปจนถึงการสร้างอ็อบเจกต์ Order ที่สมบูรณ์
        // ทำให้ OrderServiceImpl มีความกระชับและดูแลเฉพาะภาพรวมของ Transaction
        Order order = orderHelper.createAndValidateBaseOrder(request, currentUser);

        // === [CO-3] ตั้งค่ารายละเอียดการชำระเงิน ===
        // นำ Order ที่ได้จาก Helper มากำหนดวิธีการชำระเงินตามที่ผู้ใช้เลือก
        order.setPaymentDetails(PaymentDetails.builder()
                .paymentMethod(request.getPaymentMethod())
                .build());

        // === [CO-4] แยกกระบวนการตามวิธีการชำระเงิน ===
        // ตรวจสอบว่าผู้ใช้เลือกจ่ายเงินแบบใด และดำเนินการในขั้นตอนต่อไปให้ถูกต้อง
        switch (request.getPaymentMethod()) {
            case PAYPAL:
                // --- [CO-4.1] กรณีชำระผ่าน PayPal: เริ่มต้นกระบวนการกับ PayPal ---
                return initiatePaypalPayment(order);
            case BANK_TRANSFER:
                // --- [CO-4.2] กรณีโอนเงิน: บันทึก Order และรอสลิป ---
                orderRepository.save(order);
                log.info("Saved new BANK_TRANSFER order with ID: {}", order.getId());
                return new CreateOrderResponse(order.getId());
            default:
                // --- [CO-4.3] กรณีไม่รองรับ: แจ้งข้อผิดพลาด ---
                log.error("Unsupported payment method received: {}", request.getPaymentMethod());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method.");
        }
    }

    @Override
    @Transactional
    public OrderResponse capturePaypalOrder(String orderId, String paymentId, String payerId) throws PayPalRESTException {
        log.info("Attempting to capture PayPal payment for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("PayPal callback error: Order not found with ID: {}", orderId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
                });

        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.PAYPAL) {
            log.error("PayPal callback error: Order {} is not a PayPal order or missing payment details.", orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not designated for PayPal payment or is missing payment details.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("PayPal callback warning: Attempt to capture an already processed or non-pending order. Order ID: {}, Status: {}", orderId, order.getPaymentStatus());
            return orderHelper.entityToResponse(order);
        }

        Payment payment = paypalService.executePayment(paymentId, payerId);

        if ("approved".equals(payment.getState())) {
            orderHelper.decrementStockForOrder(order);

            PaymentDetails details = order.getPaymentDetails();
            details.setTransactionId(payment.getId());
            details.setPayerId(payment.getPayer().getPayerInfo().getPayerId());
            details.setPayerEmail(payment.getPayer().getPayerInfo().getEmail());
            details.setProviderStatus(payment.getState());

            updateOrderStatusToPaid(order);
            log.info("Successfully captured PayPal payment for order ID: {}", orderId);
            return orderHelper.entityToResponse(orderRepository.save(order));
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            log.error("PayPal payment capture failed for order ID: {}. State: {}", orderId, payment.getState());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment could not be approved by PayPal. State: " + payment.getState());
        }
    }

    @Override
    @Transactional
    public OrderResponse submitPaymentSlip(String orderId, MultipartFile slipImage) {
        if (slipImage == null || slipImage.isEmpty()) {
            log.warn("Attempt to submit slip for order {} without an image file.", orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment slip image is required.");
        }
        String userId = userService.findByUserId();
        Order order = orderHelper.findOrderForProcessing(orderId, userId, PaymentMethod.BANK_TRANSFER);

        PaymentDetails details = order.getPaymentDetails();
        if (details == null) {
            log.warn("PaymentDetails was null for order {} during slip submission. Creating new.", orderId);
            details = PaymentDetails.builder().paymentMethod(PaymentMethod.BANK_TRANSFER).build();
            order.setPaymentDetails(details);
        }

        String oldSlipUrl = details.getTransactionId();
        if (oldSlipUrl != null && !oldSlipUrl.isBlank() && oldSlipUrl.contains("s3.amazonaws.com")) {
            try {
                String oldFilename = oldSlipUrl.substring(oldSlipUrl.lastIndexOf("/") + 1);
                log.info("Order {} has an existing slip. Attempting to delete file: {} from S3.", orderId, oldFilename);
                s3Service.deleteFile(oldFilename);
            } catch (Exception e) {
                log.error("Error processing or deleting old slip URL '{}' for order {}: {}", oldSlipUrl, orderId, e.getMessage());
            }
        }

        String newSlipImageUrl = s3Service.uploadFile(slipImage);
        log.info("New payment slip uploaded for order {}. URL: {}", orderId, newSlipImageUrl);

        details.setTransactionId(newSlipImageUrl);
        details.setProviderStatus("SUBMITTED");
        order.setPaymentStatus(PaymentStatus.PENDING_APPROVAL);
        order.setUpdatedAt(Instant.now());

        orderRepository.save(order);
        log.info("Payment slip submitted and order {} updated. Awaiting admin approval.", orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    public OrderResponse getOrderById(String orderId) {
        String currentUserId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (!order.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order.");
        }
        return orderHelper.entityToResponse(order);
    }

    @Override
    public List<OrderResponse> getCurrentUserOrders() {
        String userId = userService.findByUserId();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderHelper::entityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING || order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("User {} attempted to cancel order {} with invalid status: Payment={}, Order={}", userId, orderId, order.getPaymentStatus(), order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel an order that is not pending payment.");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Order ID {} has been cancelled by user {}.", orderId, userId);

        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public CreateOrderResponse retryPayment(String orderId) throws PayPalRESTException {
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order.");
        }

        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.PAYPAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retry payment is only available for PayPal orders.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING && order.getPaymentStatus() != PaymentStatus.FAILED) {
            log.warn("User {} attempted to retry payment for order {} with status: {}", userId, orderId, order.getPaymentStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot retry payment for this order. Current status: " + order.getPaymentStatus());
        }

        log.info("Retrying PayPal payment for order ID: {}", orderId);
        return initiatePaypalPayment(order);
    }

    @Override
    @Transactional
    public OrderResponse requestRefund(String orderId) {
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You do not own this order.");
        }

        List<OrderStatus> validStatusesForRefundRequest = List.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.COMPLETED);
        if (!validStatusesForRefundRequest.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot request refund for an order with status: " + order.getOrderStatus());
        }
        if (order.getOrderStatus() == OrderStatus.REFUND_REQUESTED || order.getOrderStatus() == OrderStatus.REFUNDED || order.getOrderStatus() == OrderStatus.REFUND_REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A refund request for this order already exists or has been processed.");
        }

        order.setOrderStatus(OrderStatus.REFUND_REQUESTED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("User {} successfully requested a refund for order ID: {}", userId, orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse approveRefund(String orderId) throws PayPalRESTException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (order.getOrderStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not awaiting a refund approval.");
        }

        PaymentDetails paymentDetails = order.getPaymentDetails();
        if (paymentDetails == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment details are missing for this order.");
        }

        if (paymentDetails.getPaymentMethod() == PaymentMethod.PAYPAL) {
            orderHelper.processPaypalRefund(order, paymentDetails);
        } else if (paymentDetails.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            paymentDetails.setProviderStatus("MANUALLY_REFUNDED_APPROVED");
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported payment method for refund.");
        }

        orderHelper.incrementStockForOrder(order);

        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setUpdatedAt(Instant.now());

        orderRepository.save(order);
        log.info("Refund for order ID: {} has been approved and processed by admin.", orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse shipOrder(String orderId, ShipOrderRequest request) {
        // === [SHIP-1] เริ่มกระบวนการจัดส่ง: ค้นหาออเดอร์ ===
        log.info("Attempting to ship order with ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [SHIP-2] ตรวจสอบสถานะของออเดอร์ ===
        if (order.getOrderStatus() != OrderStatus.PROCESSING) {
            log.warn("Attempted to ship an order with invalid status. Order ID: {}, Status: {}", orderId, order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be shipped. Current status is: " + order.getOrderStatus());
        }

        // === [SHIP-3] สร้างอ็อบเจกต์ ShippingDetails จากข้อมูลที่ได้รับ ===
        ShippingDetails shippingDetails = ShippingDetails.builder()
                .shippingProvider(request.getShippingProvider())
                .trackingNumber(request.getTrackingNumber())
                .shippedAt(Instant.now())
                .build();

        // === [SHIP-4] อัปเดตข้อมูลในออเดอร์ ===
        order.setShippingDetails(shippingDetails);
        order.setOrderStatus(OrderStatus.SHIPPED);
        order.setUpdatedAt(Instant.now());

        // === [SHIP-5] บันทึกออเดอร์ที่อัปเดตแล้วลงฐานข้อมูล ===
        orderRepository.save(order);
        log.info("Order ID {} has been marked as SHIPPED via {} with tracking number: {}",
                orderId, request.getShippingProvider(), request.getTrackingNumber());

        // === [SHIP-6] ส่งคืนข้อมูลออเดอร์ที่อัปเดตแล้ว ===
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse rejectRefund(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (order.getOrderStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not awaiting a refund approval.");
        }

        order.setOrderStatus(OrderStatus.REFUND_REJECTED);
        if (order.getPaymentDetails() != null) {
            order.getPaymentDetails().setProviderStatus("REFUND_REJECTED_BY_ADMIN");
        }
        order.setUpdatedAt(Instant.now());

        orderRepository.save(order);
        log.info("Refund request for order ID: {} has been rejected by an admin.", orderId);
        return orderHelper.entityToResponse(order);
    }

    private CreateOrderResponse initiatePaypalPayment(Order order) throws PayPalRESTException {
        if (order.getId() == null) {
            orderRepository.save(order);
            log.info("Order ID {} generated and saved before initiating PayPal payment.", order.getId());
        }

        String formattedSuccessUrl = String.format(successUrl, order.getId());
        String formattedCancelUrl = String.format(cancelUrl, order.getId());

        Payment payment = paypalService.createPayment(
                order.getTotalAmount(),
                order.getCurrency(),
                "sale",
                "Order #" + order.getId(),
                formattedCancelUrl,
                formattedSuccessUrl
        );

        String approvalLink = "";
        for (Links link : payment.getLinks()) {
            if ("approval_url".equals(link.getRel())) {
                approvalLink = link.getHref();
                break;
            }
        }
        String paypalPaymentId = payment.getId();

        if (approvalLink.isEmpty() || paypalPaymentId == null) {
            throw new PayPalRESTException("Could not get approval link or payment ID from PayPal.");
        }

        PaymentDetails details = order.getPaymentDetails();
        details.setTransactionId(paypalPaymentId);
        details.setProviderStatus("CREATED_IN_PAYPAL");

        orderRepository.save(order);
        log.info("Updated order ID {} with PayPal Payment ID: {}", order.getId(), paypalPaymentId);

        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .paypalOrderId(paypalPaymentId)
                .approvalLink(approvalLink)
                .build();
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Admin action: Fetching all orders from the system.");
        return orderRepository.findAll()
                .stream()
                .map(orderHelper::entityToResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public OrderResponse approvePaymentSlip(String orderId) {
        log.info("Admin action: Approving payment slip for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (order.getPaymentStatus() != PaymentStatus.PENDING_APPROVAL) {
            log.warn("Attempted to approve slip for an order with invalid payment status. Order ID: {}, Status: {}", orderId, order.getPaymentStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not awaiting payment slip approval. Current status: " + order.getPaymentStatus());
        }

        orderHelper.decrementStockForOrder(order);
        updateOrderStatusToPaid(order);

        if (order.getPaymentDetails() != null) {
            order.getPaymentDetails().setProviderStatus("APPROVED_BY_ADMIN");
        }

        orderRepository.save(order);
        log.info("Payment slip for order ID {} has been approved. Stock decremented and status updated to PROCESSING.", orderId);

        return orderHelper.entityToResponse(order);
    }

    @Override
    public OrderResponse getAnyOrderByIdForAdmin(String orderId) {
        log.info("Admin action: Fetching order details for ID: {} without ownership check.", orderId);
        // This version simply finds the order and returns it, bypassing the user ID check.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        return orderHelper.entityToResponse(order);
    }

    private void updateOrderStatusToPaid(Order order) {
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setOrderStatus(OrderStatus.PROCESSING);
        order.setUpdatedAt(Instant.now());
    }
}