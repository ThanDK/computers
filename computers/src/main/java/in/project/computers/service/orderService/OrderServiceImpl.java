package in.project.computers.service.orderService;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.CreateOrderResponse;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.DTO.order.orderRequest.ShipOrderRequest;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalReposiroty.OrderRepository;
import in.project.computers.repository.generalReposiroty.UserRepository;
import in.project.computers.service.awsS3Bucket.S3Service;
import in.project.computers.service.cartService.CartService;
import in.project.computers.service.paypalService.PaypalService;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderHelperService orderHelper;
    private final UserService userService;
    private final S3Service s3Service;
    private final PaypalService paypalService;
    private final CartService cartService;

    @Value("${paypal.payment.cancelUrl}")
    private String cancelUrl;
    @Value("${paypal.payment.successUrl}")
    private String successUrl;

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) throws PayPalRESTException {
        // === [CREATE-1] ตรวจสอบและดึงข้อมูลผู้ใช้ปัจจุบัน ===
        UserEntity currentUser = userRepository.findById(userService.findByUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // === [CREATE-2] ตรวจสอบว่าตะกร้าสินค้าไม่ว่างเปล่า ===
        Cart userCart = cartService.getCartEntityByUserId(currentUser.getId());
        if (userCart == null || userCart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create an order with an empty cart.");
        }

        // === [CREATE-3] สร้างและตรวจสอบความถูกต้องของออเดอร์จากตะกร้าสินค้า ===
        Order order = orderHelper.createAndValidateOrderFromCart(userCart, request, currentUser);

        // === [CREATE-4] ตั้งค่ารายละเอียดการชำระเงินเริ่มต้น ===
        order.setPaymentDetails(PaymentDetails.builder()
                .paymentMethod(request.getPaymentMethod())
                .build());

        // === [CREATE-5] แยกกระบวนการตามวิธีการชำระเงิน ===
        CreateOrderResponse response;
        switch (request.getPaymentMethod()) {
            case PAYPAL:
                response = initiatePaypalPayment(order);
                break;
            case BANK_TRANSFER:
                orderRepository.save(order);
                log.info("Saved new BANK_TRANSFER order with ID: {}", order.getId());
                response = new CreateOrderResponse(order.getId());
                break;
            default:
                log.error("Unsupported payment method received: {}", request.getPaymentMethod());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method.");
        }

        // === [CREATE-6] ล้างตะกร้าสินค้าหลังจากสร้างออเดอร์สำเร็จ ===
        cartService.clearCart(currentUser.getId());
        log.info("Successfully created order {} and cleared the cart for user {}", order.getId(), currentUser.getId());

        return response;
    }


    @Override
    @Transactional
    public void capturePaypalOrder(String orderId, String paymentId, String payerId) throws PayPalRESTException {
        // === [PPC-1] ค้นหาออเดอร์จาก ID ที่ได้รับจาก PayPal Callback ===
        log.info("Attempting to capture PayPal payment for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("PayPal callback error: Order not found with ID: {}", orderId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
                });

        // === [PPC-2] ตรวจสอบความถูกต้องของออเดอร์ว่าเป็น PayPal และสถานะถูกต้องหรือไม่ ===
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.PAYPAL) {
            log.error("PayPal callback error: Order {} is not a PayPal order or missing payment details.", orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not designated for PayPal payment or is missing payment details.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("PayPal callback warning: Attempt to capture an already processed or non-pending order. Order ID: {}, Status: {}", orderId, order.getPaymentStatus());
            orderHelper.entityToResponse(order);
            return;
        }

        // === [PPC-3] ยืนยันการชำระเงินกับ PayPal ===
        Payment payment = paypalService.executePayment(paymentId, payerId);

        // === [PPC-4] กรณีการยืนยันสำเร็จ: อัปเดตสถานะออเดอร์และตัดสต็อก ===
        if ("approved".equals(payment.getState())) {
            // === [PPC-4.1] ตัดสต็อกสินค้า ===
            orderHelper.decrementStockForOrder(order);

            // === [PPC-4.2] อัปเดตรายละเอียดการชำระเงิน ===
            PaymentDetails details = order.getPaymentDetails();
            details.setTransactionId(payment.getId());
            details.setPayerId(payment.getPayer().getPayerInfo().getPayerId());
            details.setPayerEmail(payment.getPayer().getPayerInfo().getEmail());
            details.setProviderStatus(payment.getState());

            // === [PPC-4.3] อัปเดตสถานะออเดอร์ ===
            updateOrderStatusToPaid(order);
            log.info("Successfully captured PayPal payment for order ID: {}", orderId);
            // === [PPC-4.4] บันทึกและส่งคืนผลลัพธ์ ===
            orderHelper.entityToResponse(orderRepository.save(order));
        } else {
            // === [PPC-5] กรณีการยืนยันล้มเหลว: อัปเดตสถานะเป็น FAILED ===
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            log.error("PayPal payment capture failed for order ID: {}. State: {}", orderId, payment.getState());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment could not be approved by PayPal. State: " + payment.getState());
        }
    }

    @Override
    @Transactional
    public OrderResponse submitPaymentSlip(String orderId, MultipartFile slipImage) {
        // === [SLIP-1] ตรวจสอบว่ามีไฟล์สลิปแนบมาหรือไม่ ===
        if (slipImage == null || slipImage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment slip image is required.");
        }

        // === [SLIP-2] ค้นหาออเดอร์และตรวจสอบสถานะที่อนุญาตให้อัปโหลดสลิปได้ ===
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        boolean isInitialSubmission = order.getOrderStatus() == OrderStatus.PENDING_PAYMENT && order.getPaymentStatus() == PaymentStatus.PENDING;
        boolean isResubmissionAfterRejection = order.getOrderStatus() == OrderStatus.REJECTED_SLIP && order.getPaymentStatus() == PaymentStatus.PENDING;

        if (!isInitialSubmission && !isResubmissionAfterRejection) {
            log.warn("User {} attempted to submit a slip for order {} in an invalid state: OrderStatus={}, PaymentStatus={}",
                    userId, orderId, order.getOrderStatus(), order.getPaymentStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not in a state to accept a payment slip.");
        }

        // === [SLIP-3] ตรวจสอบความเป็นเจ้าของออเดอร์และวิธีการชำระเงิน ===
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect payment method for this action.");
        }

        // === [SLIP-4] หากมีการส่งสลิปซ้ำ ให้ลบไฟล์สลิปเก่าออกจาก S3 ===
        PaymentDetails details = order.getPaymentDetails();
        String oldSlipUrl = details.getSlipImageUrl();
        if (oldSlipUrl != null && !oldSlipUrl.isBlank() && oldSlipUrl.contains("s3.amazonaws.com")) {
            try {
                String oldFilename = oldSlipUrl.substring(oldSlipUrl.lastIndexOf("/") + 1);
                log.info("Order {} has an existing slip. Attempting to delete file: {} from S3.", orderId, oldFilename);
                s3Service.deleteFileByKey(oldFilename);
            } catch (Exception e) {
                log.error("Error processing or deleting old slip URL '{}' for order {}: {}", oldSlipUrl, orderId, e.getMessage());
            }
        }

        // === [SLIP-5] อัปโหลดสลิปใหม่ไปที่ S3 ===
        String newSlipImageUrl = s3Service.uploadFile(slipImage);
        log.info("New payment slip uploaded for order {}. URL: {}", orderId, newSlipImageUrl);

        // === [SLIP-6] อัปเดตข้อมูลสลิปและสถานะ ===
        details.setSlipImageUrl(newSlipImageUrl);
        details.setProviderStatus("SUBMITTED");
        details.setSlipRejectionReason(null); // ล้างเหตุผลการปฏิเสธเก่าออกไปเมื่อมีการส่งสลิปใหม่

        // === [SLIP-7] รีเซ็ตสถานะเพื่อเข้าสู่กระบวนการอนุมัติอีกครั้ง ===
        // การตั้งค่านี้จะทำให้ออเดอร์กลับเข้าสู่ Flow การตรวจสอบตามปกติ ไม่ว่าจะส่งครั้งแรกหรือส่งซ้ำ
        order.setPaymentStatus(PaymentStatus.PENDING_APPROVAL);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT); // รีเซ็ตสถานะออเดอร์หลักเพื่อความสอดคล้องกัน
        order.setUpdatedAt(Instant.now());

        // === [SLIP-8] บันทึกและส่งคืนข้อมูลออเดอร์ที่อัปเดตแล้ว ===
        orderRepository.save(order);
        log.info("Payment slip submitted/re-submitted and order {} updated. Awaiting admin approval.", orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    public OrderResponse getOrderById(String orderId) {
        // === [GET-1] ดึง ID ผู้ใช้ปัจจุบัน ===
        String currentUserId = userService.findByUserId();
        // === [GET-2] ค้นหาออเดอร์ ===
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [GET-3] ตรวจสอบความเป็นเจ้าของออเดอร์ ===
        if (!order.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order.");
        }
        // === [GET-4] แปลง Entity เป็น Response DTO และส่งคืน ===
        return orderHelper.entityToResponse(order);
    }

    @Override
    public List<OrderResponse> getCurrentUserOrders() {
        // === [GET-ALL-USER-1] ดึง ID ผู้ใช้ปัจจุบัน ===
        String userId = userService.findByUserId();
        // === [GET-ALL-USER-2] ค้นหาออเดอร์ทั้งหมดของผู้ใช้และเรียงจากใหม่ไปเก่า ===
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderHelper::entityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        // === [CANCEL-1] ดึงข้อมูลผู้ใช้และออเดอร์ ===
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        // === [CANCEL-2] ตรวจสอบสิทธิ์และความถูกต้องของสถานะออเดอร์ ===
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING || order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("User {} attempted to cancel order {} with invalid status: Payment={}, Order={}", userId, orderId, order.getPaymentStatus(), order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel an order that is not pending payment.");
        }

        // === [CANCEL-3] อัปเดตสถานะเป็น "ยกเลิก" ===
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
        // === [RETRY-1] ดึงข้อมูลผู้ใช้และออเดอร์ ===
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [RETRY-2] ตรวจสอบสิทธิ์และความถูกต้องของออเดอร์ ===
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

        // === [RETRY-3] เรียกใช้กระบวนการสร้างลิงก์ PayPal ใหม่อีกครั้ง ===
        log.info("Retrying PayPal payment for order ID: {}", orderId);
        return initiatePaypalPayment(order);
    }

    @Override
    @Transactional
    public OrderResponse requestRefund(String orderId) {
        // === [REFUND-REQ-1] ดึงข้อมูลผู้ใช้และออเดอร์ ===
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [REFUND-REQ-2] ตรวจสอบสิทธิ์ความเป็นเจ้าของออเดอร์ ===
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You do not own this order.");
        }

        // === [REFUND-REQ-3] ตรวจสอบสถานะที่สามารถขอคืนเงินได้ ===
        List<OrderStatus> validStatusesForRefundRequest = List.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.COMPLETED);
        if (!validStatusesForRefundRequest.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot request refund for an order with status: " + order.getOrderStatus());
        }
        if (order.getOrderStatus() == OrderStatus.REFUND_REQUESTED || order.getOrderStatus() == OrderStatus.REFUNDED || order.getOrderStatus() == OrderStatus.REFUND_REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A refund request for this order already exists or has been processed.");
        }

        // === [REFUND-REQ-4] อัปเดตสถานะเป็น "ร้องขอคืนเงิน" ===
        order.setOrderStatus(OrderStatus.REFUND_REQUESTED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("User {} successfully requested a refund for order ID: {}", userId, orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse approveRefund(String orderId) throws PayPalRESTException {
        // === [APPROVE-REFUND-1] ค้นหาออเดอร์ ===
        log.info("Admin is approving a user's refund request for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [APPROVE-REFUND-2] ตรวจสอบว่าออเดอร์อยู่ในสถานะที่รอการอนุมัติคืนเงินจริง ===
        if (order.getOrderStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not awaiting a refund approval. Use 'force refund' for other statuses.");
        }

        // === [APPROVE-REFUND-3] เรียกใช้เมธอดกลางเพื่อดำเนินการคืนเงิน ===
        return processRefund(order);
    }

    @Override
    @Transactional
    public OrderResponse forceRefundByAdmin(String orderId) throws PayPalRESTException {
        // === [FORCE-REFUND-1] ค้นหาออเดอร์ ===
        log.info("Admin is forcing a refund for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [FORCE-REFUND-2] ตรวจสอบว่าสถานะปัจจุบันสามารถบังคับคืนเงินได้หรือไม่ ===
        List<OrderStatus> validStatusesForForceRefund = List.of(
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED,
                OrderStatus.COMPLETED,
                OrderStatus.DELIVERY_FAILED,
                OrderStatus.RETURNED_TO_SENDER,
                OrderStatus.REFUND_REJECTED
        );

        if (!validStatusesForForceRefund.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot force a refund on an order with status: " + order.getOrderStatus());
        }

        // === [FORCE-REFUND-3] เรียกใช้เมธอดกลางเพื่อดำเนินการคืนเงิน ===
        return processRefund(order);
    }

    @Override
    @Transactional
    public OrderResponse shipOrder(String orderId, ShipOrderRequest request) {
        // === [SHIP-1] เริ่มกระบวนการจัดส่ง: ค้นหาออเดอร์ ===
        log.info("Attempting to ship order with ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [SHIP-2] ตรวจสอบสถานะของออเดอร์ว่าพร้อมจัดส่งหรือไม่ ===
        List<OrderStatus> shippableStatuses = List.of(OrderStatus.PROCESSING, OrderStatus.RETURNED_TO_SENDER);
        if (!shippableStatuses.contains(order.getOrderStatus())) {
            log.warn("Attempted to ship an order with invalid status. Order ID: {}, Status: {}", orderId, order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be shipped. Current status is: " + order.getOrderStatus());
        }

        // === [SHIP-3] สร้างอ็อบเจกต์ ShippingDetails จากข้อมูลที่ได้รับ ===
        ShippingDetails shippingDetails = ShippingDetails.builder()
                .shippingProvider(request.getShippingProvider())
                .trackingNumber(request.getTrackingNumber())
                .shippedAt(Instant.now())
                .build();

        // === [SHIP-4] อัปเดตข้อมูลการจัดส่งและสถานะในออเดอร์ ===
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
    public OrderResponse updateShippingDetails(String orderId, ShipOrderRequest request) {
        // === [SHIP-UPDATE-1] ค้นหาออเดอร์ ===
        log.info("Attempting to update shipping details for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [SHIP-UPDATE-2] ตรวจสอบสถานะว่าสามารถอัปเดตข้อมูลการจัดส่งได้ ===
        if (order.getOrderStatus() != OrderStatus.SHIPPED && order.getOrderStatus() != OrderStatus.COMPLETED) {
            log.warn("Attempted to update shipping info for an order with invalid status. Order ID: {}, Status: {}",
                    orderId, order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot update shipping details. The order has not been shipped yet. Current status: " + order.getOrderStatus());
        }

        // === [SHIP-UPDATE-3] ตรวจสอบความสอดคล้องของข้อมูล ShippingDetails ===
        if (order.getShippingDetails() == null) {
            log.error("Data inconsistency: Order {} is SHIPPED but has no ShippingDetails object.", orderId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot update shipping details because they were never set in the first place.");
        }

        // === [SHIP-UPDATE-4] อัปเดตข้อมูลการจัดส่ง ===
        ShippingDetails shippingDetails = order.getShippingDetails();
        shippingDetails.setShippingProvider(request.getShippingProvider());
        shippingDetails.setTrackingNumber(request.getTrackingNumber());
        order.setUpdatedAt(Instant.now());

        // === [SHIP-UPDATE-5] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Successfully updated shipping details for order ID {}. New provider: {}, New tracking: {}",
                orderId, request.getShippingProvider(), request.getTrackingNumber());

        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus) {
        // === [STATUS-UPDATE-1] ค้นหาออเดอร์ ===
        log.info("Attempting to manually update status for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getOrderStatus();
        log.info("Admin request to transition order {} from [{}] to [{}]", orderId, currentStatus, newStatus);

        // === [STATUS-UPDATE-2] ตรวจสอบว่าการเปลี่ยนสถานะถูกต้องตาม Flow ที่กำหนดหรือไม่ ===
        List<OrderStatus> validTransitions = getValidManualTransitionsFor(currentStatus);
        if (!validTransitions.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status transition from '" + currentStatus + "' to '" + newStatus + "'.");
        }

        // === [STATUS-UPDATE-3] จัดการ Logic พิเศษสำหรับบางกรณีการเปลี่ยนสถานะ ===
        if (currentStatus == OrderStatus.PENDING_PAYMENT && newStatus == OrderStatus.CANCELLED && order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        // === [STATUS-UPDATE-4] อัปเดตสถานะและบันทึก ===
        order.setOrderStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Successfully updated status for order ID {} to {}", orderId, newStatus);

        return orderHelper.entityToResponse(order);
    }

    @Override
    public List<OrderStatus> getValidNextStatuses(String orderId) {
        // === [GET-STATUS-1] ค้นหาออเดอร์ ===
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        // === [GET-STATUS-2] เรียกใช้ Logic กลางเพื่อดูสถานะถัดไปที่เป็นไปได้ ===
        return getValidManualTransitionsFor(order.getOrderStatus());
    }

    @Override
    @Transactional
    public OrderResponse rejectRefund(String orderId) {
        // === [REFUND-REJ-1] ค้นหาออเดอร์ ===
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [REFUND-REJ-2] ตรวจสอบว่าออเดอร์อยู่ในสถานะรอการอนุมัติคืนเงินหรือไม่ ===
        if (order.getOrderStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not awaiting a refund approval.");
        }

        // === [REFUND-REJ-3] อัปเดตสถานะเป็น "ปฏิเสธการคืนเงิน" ===
        order.setOrderStatus(OrderStatus.REFUND_REJECTED);
        if (order.getPaymentDetails() != null) {
            order.getPaymentDetails().setProviderStatus("REFUND_REJECTED_BY_ADMIN");
        }
        order.setUpdatedAt(Instant.now());

        // === [REFUND-REJ-4] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Refund request for order ID: {} has been rejected by an admin.", orderId);
        return orderHelper.entityToResponse(order);
    }

    // --- Private Helper Methods ---

    /**
     * เมธอดภายในสำหรับจัดการกระบวนการสร้าง Payment กับ PayPal และอัปเดตออเดอร์
     */
    private CreateOrderResponse initiatePaypalPayment(Order order) throws PayPalRESTException {
        // === [INIT-PAYPAL-1] บันทึกออเดอร์เพื่อสร้าง ID หากยังไม่มี ===
        if (order.getId() == null) {
            orderRepository.save(order);
            log.info("Order ID {} generated and saved before initiating PayPal payment.", order.getId());
        }

        // === [INIT-PAYPAL-2] เตรียม URL สำหรับ Success และ Cancel ===
        String formattedSuccessUrl = String.format(successUrl, order.getId());
        String formattedCancelUrl = String.format(cancelUrl, order.getId());

        // === [INIT-PAYPAL-3] เรียกใช้ PaypalService เพื่อสร้าง Payment ===
        Payment payment = paypalService.createPayment(
                order,
                "sale",
                "Order #" + order.getId(),
                formattedCancelUrl,
                formattedSuccessUrl
        );

        // === [INIT-PAYPAL-4] ดึงลิงก์สำหรับให้ผู้ใช้อนุมัติ (Approval Link) ===
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

        // === [INIT-PAYPAL-5] อัปเดตออเดอร์ด้วย Payment ID จาก PayPal ===
        PaymentDetails details = order.getPaymentDetails();
        details.setTransactionId(paypalPaymentId);
        details.setProviderStatus("CREATED_IN_PAYPAL");
        orderRepository.save(order);
        log.info("Updated order ID {} with PayPal Payment ID: {}", order.getId(), paypalPaymentId);

        // === [INIT-PAYPAL-6] สร้าง Response เพื่อส่งคืนให้แก่ Frontend ===
        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .paypalOrderId(paypalPaymentId)
                .approvalLink(approvalLink)
                .build();
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        // === [GET-ALL-ADMIN-1] ดึงออเดอร์ทั้งหมดจากฐานข้อมูลสำหรับ Admin ===
        log.info("Admin action: Fetching all orders from the system.");
        return orderRepository.findAll()
                .stream()
                .map(orderHelper::entityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse approvePaymentSlip(String orderId) {
        // === [APPROVE-SLIP-1] ค้นหาออเดอร์ ===
        log.info("Admin action: Approving payment slip for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [APPROVE-SLIP-2] ตรวจสอบสถานะว่ารอการอนุมัติสลิปจริงหรือไม่ ===
        if (order.getPaymentStatus() != PaymentStatus.PENDING_APPROVAL) {
            log.warn("Attempted to approve slip for an order with invalid payment status. Order ID: {}, Status: {}", orderId, order.getPaymentStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not awaiting payment slip approval. Current status: " + order.getPaymentStatus());
        }

        // === [APPROVE-SLIP-3] ตัดสต็อกสินค้า ===
        orderHelper.decrementStockForOrder(order);

        // === [APPROVE-SLIP-4] อัปเดตสถานะเป็น "ชำระเงินแล้ว" และ "กำลังดำเนินการ" ===
        updateOrderStatusToPaid(order);

        if (order.getPaymentDetails() != null) {
            PaymentDetails details = order.getPaymentDetails();
            details.setProviderStatus("APPROVED");
            details.setSlipRejectionReason(null); // ล้างเหตุผลการปฏิเสธเมื่ออนุมัติสำเร็จ
        }

        // === [APPROVE-SLIP-5] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Payment slip for order ID {} has been approved. Stock decremented and status updated to PROCESSING.", orderId);

        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse rejectPaymentSlip(String orderId, String reason) {
        // === [REJECT-SLIP-1] ค้นหาออเดอร์และตรวจสอบสถานะที่สามารถปฏิเสธสลิปได้ ===
        log.info("Admin action: Rejecting payment slip for order ID: {} with reason: {}", orderId, reason);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (order.getPaymentStatus() != PaymentStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reject slip. Order is not awaiting payment approval.");
        }

        // === [REJECT-SLIP-2] เปลี่ยนสถานะเพื่อให้ผู้ใช้สามารถอัปโหลดสลิปใหม่ได้ ===
        order.setOrderStatus(OrderStatus.REJECTED_SLIP);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setUpdatedAt(Instant.now());

        // === [REJECT-SLIP-3] บันทึกเหตุผลที่ปฏิเสธในฟิลด์เฉพาะ ===
        if (order.getPaymentDetails() != null) {
            PaymentDetails details = order.getPaymentDetails();
            details.setProviderStatus("REJECTED");
            details.setSlipRejectionReason(reason);
        }

        // === [REJECT-SLIP-4] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Payment slip for order ID {} has been rejected. Awaiting user action.", orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse revertSlipApproval(String orderId, String reason) {
        // === [REVERT-1] ค้นหาออเดอร์และตรวจสอบว่าสามารถย้อนกลับการอนุมัติได้หรือไม่ ===
        log.info("Admin action: Reverting slip approval for order ID: {} with reason: {}", orderId, reason);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (order.getOrderStatus() != OrderStatus.PROCESSING || order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot revert approval. Order is not in a valid state for this action.");
        }

        // === [REVERT-2] คืนสต็อกสินค้าเข้าระบบ ===
        orderHelper.incrementStockForOrder(order);

        // === [REVERT-3] เปลี่ยนสถานะกลับไปรอให้ผู้ใช้อัปโหลดสลิปใหม่ ===
        order.setOrderStatus(OrderStatus.REJECTED_SLIP);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setUpdatedAt(Instant.now());

        // === [REVERT-4] บันทึกเหตุผลที่ย้อนกลับการอนุมัติ ===
        PaymentDetails details = order.getPaymentDetails();
        details.setProviderStatus("APPROVAL_REVERTED");
        details.setSlipRejectionReason(reason);

        // === [REVERT-5] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Approval for order {} reverted. Stock has been incremented back. Awaiting user action.", orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    public OrderResponse getAnyOrderByIdForAdmin(String orderId) {
        // === [GET-ANY-ADMIN-1] ดึงออเดอร์โดยไม่ตรวจสอบความเป็นเจ้าของสำหรับ Admin ===
        log.info("Admin action: Fetching order details for ID: {} without ownership check.", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        return orderHelper.entityToResponse(order);
    }

    /**
     * เมธอดภายในสำหรับเปลี่ยนสถานะออเดอร์เป็นสถานะที่หมายถึง "ชำระเงินเรียบร้อยแล้ว"
     */
    private void updateOrderStatusToPaid(Order order) {
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setOrderStatus(OrderStatus.PROCESSING);
        order.setUpdatedAt(Instant.now());
    }

    /**
     * เมธอดกลางสำหรับจัดการกระบวนการคืนเงิน (Refund) ทั้ง PayPal และ Bank Transfer
     */
    private OrderResponse processRefund(Order order) throws PayPalRESTException {
        // === [PROCESS-REFUND-1] ตรวจสอบว่ามี PaymentDetails หรือไม่ ===
        PaymentDetails paymentDetails = order.getPaymentDetails();
        if (paymentDetails == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment details are missing for this order.");
        }

        // === [PROCESS-REFUND-2] แยกกระบวนการคืนเงินตาม Payment Method ===
        if (paymentDetails.getPaymentMethod() == PaymentMethod.PAYPAL) {
            orderHelper.processPaypalRefund(order, paymentDetails);
        } else if (paymentDetails.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            // สำหรับ Bank Transfer เป็นการบันทึกสถานะว่าคืนเงินแล้ว (Admin ต้องจัดการคืนเงินเอง)
            if (order.getOrderStatus() == OrderStatus.REFUND_REQUESTED) {
                paymentDetails.setProviderStatus("MANUALLY_REFUNDED_APPROVED");
            } else {
                paymentDetails.setProviderStatus("MANUALLY_REFUNDED_BY_ADMIN");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported payment method for refund.");
        }

        // === [PROCESS-REFUND-3] คืนสต็อกสินค้าเข้าระบบ ===
        orderHelper.incrementStockForOrder(order);

        // === [PROCESS-REFUND-4] อัปเดตสถานะสุดท้ายของออเดอร์เป็น "คืนเงินแล้ว" ===
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setUpdatedAt(Instant.now());

        // === [PROCESS-REFUND-5] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Refund for order ID: {} has been successfully processed.", order.getId());
        return orderHelper.entityToResponse(order);
    }

    /**
     * กำหนด Logic ของการเปลี่ยนสถานะด้วยตนเองโดย Admin ว่าจากสถานะปัจจุบัน
     * สามารถเปลี่ยนไปเป็นสถานะใดได้บ้าง
     */
    private List<OrderStatus> getValidManualTransitionsFor(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING_PAYMENT, REJECTED_SLIP -> List.of(OrderStatus.CANCELLED);
            case PROCESSING, SHIPPED, DELIVERY_FAILED -> Stream.of(
                            OrderStatus.COMPLETED,
                            OrderStatus.DELIVERY_FAILED,
                            OrderStatus.RETURNED_TO_SENDER

                    )
                    .filter(status -> status != currentStatus)
                    .toList();

            case RETURNED_TO_SENDER -> List.of(
                    OrderStatus.PROCESSING
            );

            case REFUND_REJECTED -> List.of(
                    OrderStatus.COMPLETED,
                    OrderStatus.PROCESSING
            );
            default -> List.of();
        };
    }
}