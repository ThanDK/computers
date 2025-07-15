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
import java.util.stream.Stream;

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
        // === [PPC-1] เริ่มกระบวนการ Capture: ค้นหาออเดอร์จาก ID ที่ได้จาก Callback ===
        log.info("Attempting to capture PayPal payment for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("PayPal callback error: Order not found with ID: {}", orderId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
                });

        // === [PPC-2] ตรวจสอบความถูกต้องของออเดอร์ ===
        // เช็คว่าเป็นออเดอร์ PayPal และมี PaymentDetails ถูกต้องหรือไม่
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.PAYPAL) {
            log.error("PayPal callback error: Order {} is not a PayPal order or missing payment details.", orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not designated for PayPal payment or is missing payment details.");
        }
        // เช็คว่าสถานะการชำระเงินยังเป็น PENDING เพื่อป้องกันการประมวลผลซ้ำ
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("PayPal callback warning: Attempt to capture an already processed or non-pending order. Order ID: {}, Status: {}", orderId, order.getPaymentStatus());
            return orderHelper.entityToResponse(order); // ส่งคืนสถานะปัจจุบันไปเลย
        }

        // === [PPC-3] ยืนยันการชำระเงินกับ PayPal ===
        Payment payment = paypalService.executePayment(paymentId, payerId);

        // === [PPC-4] ประมวลผลหลังจากการยืนยันสำเร็จ ===
        if ("approved".equals(payment.getState())) {
            // === [PPC-4.1] ตัดสต็อกสินค้า ===
            orderHelper.decrementStockForOrder(order);

            // === [PPC-4.2] อัปเดตรายละเอียดการชำระเงินจาก PayPal ===
            PaymentDetails details = order.getPaymentDetails();
            details.setTransactionId(payment.getId()); // บันทึก PayPal Payment ID
            details.setPayerId(payment.getPayer().getPayerInfo().getPayerId());
            details.setPayerEmail(payment.getPayer().getPayerInfo().getEmail());
            details.setProviderStatus(payment.getState()); // บันทึกสถานะ "approved"

            // === [PPC-4.3] อัปเดตสถานะออเดอร์เป็น "ชำระเงินแล้ว" และ "กำลังดำเนินการ" ===
            updateOrderStatusToPaid(order);
            log.info("Successfully captured PayPal payment for order ID: {}", orderId);
            return orderHelper.entityToResponse(orderRepository.save(order));
        } else {
            // === [PPC-5] กรณีการยืนยันล้มเหลว ===
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            log.error("PayPal payment capture failed for order ID: {}. State: {}", orderId, payment.getState());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment could not be approved by PayPal. State: " + payment.getState());
        }
    }

    @Override
    @Transactional
    public OrderResponse submitPaymentSlip(String orderId, MultipartFile slipImage) {
        // === [SLIP-1] ตรวจสอบไฟล์สลิป ===
        if (slipImage == null || slipImage.isEmpty()) {
            log.warn("Attempt to submit slip for order {} without an image file.", orderId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment slip image is required.");
        }
        // === [SLIP-2] ค้นหาและตรวจสอบออเดอร์ ===
        // ใช้ Helper เพื่อหาออเดอร์, ตรวจสอบความเป็นเจ้าของ, และเช็คว่าเป็นประเภท BANK_TRANSFER ที่ถูกต้อง
        String userId = userService.findByUserId();
        Order order = orderHelper.findOrderForProcessing(orderId, userId, PaymentMethod.BANK_TRANSFER);

        // === [SLIP-3] จัดการ PaymentDetails ===
        PaymentDetails details = order.getPaymentDetails();
        if (details == null) {
            log.warn("PaymentDetails was null for order {} during slip submission. Creating new.", orderId);
            details = PaymentDetails.builder().paymentMethod(PaymentMethod.BANK_TRANSFER).build();
            order.setPaymentDetails(details);
        }

        // === [SLIP-4] [ปรับปรุง] ลบสลิปเก่า (ถ้ามี) ===
        // หากมีการส่งสลิปใหม่มาแทนที่สลิปเก่า ให้ทำการลบไฟล์สลิปเก่าออกจาก S3 เพื่อไม่ให้มีไฟล์ขยะในระบบ
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

        // === [SLIP-5] อัปโหลดสลิปใหม่ไปที่ S3 ===
        String newSlipImageUrl = s3Service.uploadFile(slipImage);
        log.info("New payment slip uploaded for order {}. URL: {}", orderId, newSlipImageUrl);

        // === [SLIP-6] อัปเดตข้อมูลในออเดอร์ ===
        details.setTransactionId(newSlipImageUrl); // เก็บ URL ของสลิปใหม่
        details.setProviderStatus("SUBMITTED"); // ตั้งสถานะภายในว่าส่งแล้ว
        order.setPaymentStatus(PaymentStatus.PENDING_APPROVAL); // ตั้งสถานะรอการตรวจสอบจากแอดมิน
        order.setUpdatedAt(Instant.now());

        // === [SLIP-7] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Payment slip submitted and order {} updated. Awaiting admin approval.", orderId);
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
                .map(orderHelper::entityToResponse) // แปลงแต่ละออเดอร์
                .collect(Collectors.toList()); // รวบรวมเป็น List
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        // === [CANCEL-1] ดึงข้อมูลผู้ใช้และออเดอร์ ===
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        // === [CANCEL-2] ตรวจสอบสิทธิ์และความถูกต้อง ===
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        // อนุญาตให้ยกเลิกได้เฉพาะออเดอร์ที่ยังไม่จ่ายเงินเท่านั้น
        if (order.getPaymentStatus() != PaymentStatus.PENDING || order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("User {} attempted to cancel order {} with invalid status: Payment={}, Order={}", userId, orderId, order.getPaymentStatus(), order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel an order that is not pending payment.");
        }

        // === [CANCEL-3] อัปเดตสถานะเป็น "ยกเลิก" ===
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED); // ถือว่าการชำระเงินล้มเหลว
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

        // ต้องเป็นออเดอร์ PayPal เท่านั้น
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.PAYPAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retry payment is only available for PayPal orders.");
        }
        // ต้องเป็นออเดอร์ที่ยังไม่จ่ายเงิน หรือการจ่ายเงินครั้งก่อนล้มเหลว
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

        // === [REFUND-REQ-2] ตรวจสอบสิทธิ์ ===
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You do not own this order.");
        }

        // === [REFUND-REQ-3] ตรวจสอบสถานะที่สามารถขอคืนเงินได้ ===
        // เช่น กำลังดำเนินการ, จัดส่งแล้ว, หรือเสร็จสมบูรณ์
        List<OrderStatus> validStatusesForRefundRequest = List.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.COMPLETED);
        if (!validStatusesForRefundRequest.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot request refund for an order with status: " + order.getOrderStatus());
        }
        // ป้องกันการส่งคำขอซ้ำซ้อน
        if (order.getOrderStatus() == OrderStatus.REFUND_REQUESTED || order.getOrderStatus() == OrderStatus.REFUNDED || order.getOrderStatus() == OrderStatus.REFUND_REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A refund request for this order already exists or has been processed.");
        }

        // === [REFUND-REQ-4] อัปเดตสถานะเป็น "ส่งคำขอคืนเงิน" ===
        order.setOrderStatus(OrderStatus.REFUND_REQUESTED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("User {} successfully requested a refund for order ID: {}", userId, orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse approveRefund(String orderId) throws PayPalRESTException {
        // === [REFUND-APP-1] ค้นหาออเดอร์ (Admin action, ไม่ต้องเช็คเจ้าของ) ===
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [REFUND-APP-2] ตรวจสอบสถานะว่ารอการอนุมัติคืนเงินจริงหรือไม่ ===
        if (order.getOrderStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not awaiting a refund approval.");
        }

        PaymentDetails paymentDetails = order.getPaymentDetails();
        if (paymentDetails == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment details are missing for this order.");
        }

        // === [REFUND-APP-3] แยกกระบวนการคืนเงินตามวิธีการชำระเงินเดิม ===
        if (paymentDetails.getPaymentMethod() == PaymentMethod.PAYPAL) {
            // มอบหมายให้ Helper จัดการคืนเงินผ่าน PayPal API
            orderHelper.processPaypalRefund(order, paymentDetails);
        } else if (paymentDetails.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            // กรณีโอนเงิน เป็นการอนุมัติในระบบให้ Admin ไปโอนคืนเอง
            paymentDetails.setProviderStatus("MANUALLY_REFUNDED_APPROVED");
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported payment method for refund.");
        }

        // === [REFUND-APP-4] คืนสต็อกสินค้าเข้าระบบ ===
        orderHelper.incrementStockForOrder(order);

        // === [REFUND-APP-5] อัปเดตสถานะออเดอร์เป็น "คืนเงินสำเร็จ" ===
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

        // ***************************************************************
        // ********************* START OF FIX 1 **************************
        // ***************************************************************
        // === [SHIP-2] ตรวจสอบสถานะของออเดอร์ (แก้ไข) ===
        // อนุญาตให้จัดส่งได้ถ้าสถานะเป็น PROCESSING หรือ RETURNED_TO_SENDER
        List<OrderStatus> shippableStatuses = List.of(OrderStatus.PROCESSING, OrderStatus.RETURNED_TO_SENDER);
        if (!shippableStatuses.contains(order.getOrderStatus())) {
            log.warn("Attempted to ship an order with invalid status. Order ID: {}, Status: {}", orderId, order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be shipped. Current status is: " + order.getOrderStatus());
        }
        // ***************************************************************
        // ********************** END OF FIX 1 ***************************
        // ***************************************************************

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
    public OrderResponse updateShippingDetails(String orderId, ShipOrderRequest request) {
        // === [SHIP-UPDATE-1] ค้นหาออเดอร์ ===
        log.info("Attempting to update shipping details for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        // === [SHIP-UPDATE-2] ตรวจสอบสถานะ ===
        // อนุญาตให้อัปเดตได้เฉพาะออเดอร์ที่จัดส่งไปแล้ว (SHIPPED หรือ COMPLETED)
        if (order.getOrderStatus() != OrderStatus.SHIPPED && order.getOrderStatus() != OrderStatus.COMPLETED) {
            log.warn("Attempted to update shipping info for an order with invalid status. Order ID: {}, Status: {}",
                    orderId, order.getOrderStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot update shipping details. The order has not been shipped yet. Current status: " + order.getOrderStatus());
        }

        // === [SHIP-UPDATE-3] ตรวจสอบความสอดคล้องของข้อมูล ===
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

        // === [STATUS-UPDATE-2] ตรวจสอบว่าการเปลี่ยนสถานะถูกต้องตาม Flow หรือไม่ ===
        List<OrderStatus> validTransitions = getValidManualTransitionsFor(currentStatus);
        if (!validTransitions.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status transition from '" + currentStatus + "' to '" + newStatus + "'.");
        }

        // === [STATUS-UPDATE-3] จัดการ Logic พิเศษบางกรณี ===
        // เช่น ถ้าเปลี่ยนจาก "รอจ่ายเงิน" เป็น "ยกเลิก" ให้สถานะการจ่ายเงินเป็น "ล้มเหลว" ด้วย
        // FIX: Corrected a critical logic error. The check now correctly compares PaymentStatus with PaymentStatus.COMPLETED,
        // preventing a completed payment from ever being marked as FAILED.
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
     * เมธอดภายในสำหรับจัดการกระบวนการสร้าง Payment กับ PayPal
     */
    private CreateOrderResponse initiatePaypalPayment(Order order) throws PayPalRESTException {
        // === [INIT-PAYPAL-1] บันทึกออเดอร์ก่อนเพื่อสร้าง ID ===
        // จำเป็นต้องมี Order ID ก่อนเพื่อนำไปใส่ใน Callback URL
        if (order.getId() == null) {
            orderRepository.save(order);
            log.info("Order ID {} generated and saved before initiating PayPal payment.", order.getId());
        }

        // === [INIT-PAYPAL-2] เตรียม Callback URL ===
        String formattedSuccessUrl = String.format(successUrl, order.getId());
        String formattedCancelUrl = String.format(cancelUrl, order.getId());

        // === [INIT-PAYPAL-3] เรียกใช้ PaypalService เพื่อสร้าง Payment ===
        Payment payment = paypalService.createPayment(
                order.getTotalAmount(),
                order.getCurrency(),
                "sale",
                "Order #" + order.getId(),
                formattedCancelUrl,
                formattedSuccessUrl
        );

        // === [INIT-PAYPAL-4] ดึงลิงก์สำหรับให้ผู้ใช้ไปชำระเงิน (approval_url) ===
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

        // === [INIT-PAYPAL-5] อัปเดตออเดอร์ด้วย PayPal Payment ID ===
        // เพื่อใช้ในการอ้างอิงตอน Capture Payment
        PaymentDetails details = order.getPaymentDetails();
        details.setTransactionId(paypalPaymentId);
        details.setProviderStatus("CREATED_IN_PAYPAL");

        orderRepository.save(order);
        log.info("Updated order ID {} with PayPal Payment ID: {}", order.getId(), paypalPaymentId);

        // === [INIT-PAYPAL-6] สร้าง Response ส่งกลับไปให้ Client ===
        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .paypalOrderId(paypalPaymentId)
                .approvalLink(approvalLink)
                .build();
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        // === [GET-ALL-ADMIN-1] ดึงออเดอร์ทั้งหมดจากฐานข้อมูล ===
        log.info("Admin action: Fetching all orders from the system.");
        return orderRepository.findAll()
                .stream()
                .map(orderHelper::entityToResponse) // แปลงเป็น DTO
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
            order.getPaymentDetails().setProviderStatus("APPROVED_BY_ADMIN");
        }

        // === [APPROVE-SLIP-5] บันทึกและส่งคืนข้อมูล ===
        orderRepository.save(order);
        log.info("Payment slip for order ID {} has been approved. Stock decremented and status updated to PROCESSING.", orderId);

        return orderHelper.entityToResponse(order);
    }

    @Override
    public OrderResponse getAnyOrderByIdForAdmin(String orderId) {
        // === [GET-ANY-ADMIN-1] ดึงออเดอร์โดยไม่เช็คเจ้าของ ===
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
     * กำหนด Logic ของการเปลี่ยนสถานะด้วยตนเองโดย Admin ว่าจากสถานะปัจจุบัน (currentStatus)
     * สามารถเปลี่ยนไปเป็นสถานะใดได้บ้าง
     */
    private List<OrderStatus> getValidManualTransitionsFor(OrderStatus currentStatus) {
        // ***************************************************************
        // ********************* START OF FIX 2 **************************
        // ***************************************************************
        // ใช้ Switch Expression เพื่อความกระชับ (แก้ไข)
        return switch (currentStatus) {
            // ถ้ายังไม่จ่ายเงิน แอดมินสามารถกดยกเลิกให้ได้
            case PENDING_PAYMENT -> List.of(OrderStatus.CANCELLED);

            // ถ้ากำลังดำเนินการ, จัดส่งแล้ว, หรือส่งไม่สำเร็จ แอดมินสามารถเปลี่ยนเป็น...
            case PROCESSING, SHIPPED, DELIVERY_FAILED -> Stream.of(
                            OrderStatus.COMPLETED,          // เสร็จสมบูรณ์
                            OrderStatus.DELIVERY_FAILED,    // ส่งไม่สำเร็จ (อาจไว้เปลี่ยนซ้ำ)
                            OrderStatus.RETURNED_TO_SENDER, // ตีกลับ
                            OrderStatus.CANCELLED           // ยกเลิก (กรณีพิเศษ)
                    )
                    .filter(status -> status != currentStatus) // กรองสถานะปัจจุบันออก
                    .toList();

            // ถ้าของตีกลับ แอดมินสามารถเลือกได้ว่าจะส่งใหม่ (กลับไป Processing) หรือยกเลิกเลย
            case RETURNED_TO_SENDER -> List.of(
                    OrderStatus.PROCESSING,
                    OrderStatus.CANCELLED
            );

            // ถ้าคำขอคืนเงินถูกปฏิเสธ ควรจะกลับไปสถานะเดิมที่สามารถจัดการต่อได้
            case REFUND_REJECTED -> List.of(
                    OrderStatus.COMPLETED,  // กลับไปสถานะเสร็จสมบูรณ์
                    OrderStatus.PROCESSING  // หรือกลับไปสถานะดำเนินการเพื่อส่งใหม่
            );

            // สถานะที่เป็นจุดสิ้นสุดของ Flow จะไม่สามารถเปลี่ยนต่อไปได้ด้วยตนเอง
            // REFUND_REQUESTED จะถูกจัดการผ่านปุ่ม Approve/Reject ไม่ใช่การเปลี่ยนสถานะด้วยตนเอง
            case COMPLETED, CANCELLED, REFUNDED, REFUND_REQUESTED -> List.of();

            // เพิ่ม default case เพื่อความปลอดภัย แม้ว่า Enum จะครอบคลุมทุกกรณีแล้ว
            default -> List.of();
        };
        // ***************************************************************
        // ********************** END OF FIX 2 ***************************
        // ***************************************************************
    }
}