package in.project.computers.service.orderService;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.inventory.StockConflictInfo;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.CreateOrderResponse;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.DTO.order.orderRequest.ShipOrderRequest;
import in.project.computers.entity.component.Component;
import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.lookup.ShippingProvider;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.exception.StockConflictException;
import in.project.computers.repository.componentRepository.InventoryRepository;
import in.project.computers.repository.generalReposiroty.CartRepository;
import in.project.computers.repository.generalReposiroty.ComputerBuildRepository;
import in.project.computers.repository.generalReposiroty.OrderRepository;
import in.project.computers.repository.generalReposiroty.UserRepository;
import in.project.computers.repository.lookupRepository.ShippingProviderRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
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
    private final ShippingProviderRepository shippingProviderRepository;
    private final InventoryRepository inventoryRepository;
    private final CartRepository cartRepository;
    private final ComputerBuildRepository buildRepository;

    @Value("${paypal.payment.cancelUrl}")
    private String cancelUrl;
    @Value("${paypal.payment.successUrl}")
    private String successUrl;

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) throws PayPalRESTException {
        UserEntity currentUser = userRepository.findById(userService.findByUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Cart userCart = cartService.getCartEntityByUserId(currentUser.getId());
        if (userCart == null || userCart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create an order with an empty cart.");
        }

        Map<String, Integer> stockToReReserve = new HashMap<>();
        Instant now = Instant.now();
        for (CartItem item : userCart.getItems()) {
            boolean isExpired = item.getReservationExpiresAt() == null || item.getReservationExpiresAt().isBefore(now);
            if (!item.isStockReserved() || isExpired) {
                Map<String, Integer> requiredForItem = getRequiredStockForCartItem(item.getProductId(), item.getItemType(), item.getQuantity());
                requiredForItem.forEach((key, value) -> stockToReReserve.merge(key, value, Integer::sum));
            }
        }

        if (!stockToReReserve.isEmpty()) {
            log.info("Attempting to re-reserve stock for expired items for user {}", currentUser.getId());
            List<StockConflictInfo> conflicts = inventoryRepository.attemptReservation(stockToReReserve);
            if (!conflicts.isEmpty()) {
                throw new StockConflictException(conflicts);
            }
            userCart.getItems().forEach(item -> {
                item.setStockReserved(true);

                // --- CHANGE 1 IS HERE ---
                // FROM: item.setReservationExpiresAt(now.plus(12, ChronoUnit.HOURS));
                // TO:
                item.setReservationExpiresAt(now.plus(30, ChronoUnit.SECONDS)); // FOR TESTING
            });
        }

        Order order = orderHelper.createAndValidateOrderFromCart(userCart, request, currentUser);

        order.setPaymentDetails(PaymentDetails.builder()
                .paymentMethod(request.getPaymentMethod())
                .build());

        // --- CHANGE 2 IS HERE ---
        // FROM: order.setHoldExpiresAt(now.plus(24, ChronoUnit.HOURS));
        // TO:
        order.setHoldExpiresAt(now.plus(35, ChronoUnit.SECONDS)); // FOR TESTING

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

        userCart.getItems().clear();
        cartRepository.save(userCart);
        log.info("Successfully created order {} and cleared the cart items for user {}", order.getId(), currentUser.getId());

        return response;
    }

    @Override
    @Transactional
    public void capturePaypalOrder(String orderId, String paymentId, String payerId) throws PayPalRESTException {
        log.info("Attempting to capture PayPal payment for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));

        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.PAYPAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not designated for PayPal payment.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            orderHelper.entityToResponse(order);
            return;
        }

        Payment payment = paypalService.executePayment(paymentId, payerId);

        if ("approved".equals(payment.getState())) {
            PaymentDetails details = order.getPaymentDetails();
            details.setTransactionId(payment.getId());
            details.setPayerId(payment.getPayer().getPayerInfo().getPayerId());
            details.setPayerEmail(payment.getPayer().getPayerInfo().getEmail());
            details.setProviderStatus(payment.getState());

            updateOrderStatusToPaid(order);
            log.info("Successfully captured PayPal payment for order ID: {}", orderId);
            orderHelper.entityToResponse(orderRepository.save(order));
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment slip image is required.");
        }
        String userId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        boolean isInitialSubmission = order.getOrderStatus() == OrderStatus.PENDING_PAYMENT && order.getPaymentStatus() == PaymentStatus.PENDING;
        boolean isResubmissionAfterRejection = order.getOrderStatus() == OrderStatus.REJECTED_SLIP && order.getPaymentStatus() == PaymentStatus.PENDING;
        if (!isInitialSubmission && !isResubmissionAfterRejection) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not in a state to accept a payment slip.");
        }
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect payment method for this action.");
        }
        PaymentDetails details = order.getPaymentDetails();
        String oldSlipUrl = details.getSlipImageUrl();
        if (oldSlipUrl != null && !oldSlipUrl.isBlank() && oldSlipUrl.contains("s3.amazonaws.com")) {
            try {
                String oldFilename = oldSlipUrl.substring(oldSlipUrl.lastIndexOf("/") + 1);
                s3Service.deleteFileByKey(oldFilename);
            } catch (Exception e) {
                log.error("Error processing or deleting old slip URL '{}' for order {}: {}", oldSlipUrl, orderId, e.getMessage());
            }
        }
        String newSlipImageUrl = s3Service.uploadFile(slipImage);
        details.setSlipImageUrl(newSlipImageUrl);
        details.setProviderStatus("SUBMITTED");
        details.setSlipRejectionReason(null);
        order.setPaymentStatus(PaymentStatus.PENDING_APPROVAL);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
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
    public OrderResponse cancelOrderByUser(String orderId) {
        String currentUserId = userService.findByUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
        log.info("User {} is attempting to cancel order {}", currentUserId, orderId);
        if (!order.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You do not own this order.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING || order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel an order that is not pending payment.");
        }
        orderHelper.incrementStockForOrder(order);
        log.info("Released stock for user-cancelled order ID: {}", orderId);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Order ID {} has been successfully cancelled by user {}.", orderId, currentUserId);
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot retry payment for this order. Current status: " + order.getPaymentStatus());
        }
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
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse approveRefund(String orderId, MultipartFile refundSlip) throws PayPalRESTException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        if (order.getOrderStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is not awaiting a refund approval.");
        }
        return processRefund(order, refundSlip);
    }

    @Override
    @Transactional
    public OrderResponse forceRefundByAdmin(String orderId) throws PayPalRESTException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        List<OrderStatus> validStatusesForForceRefund = List.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.COMPLETED, OrderStatus.DELIVERY_FAILED, OrderStatus.RETURNED_TO_SENDER, OrderStatus.REFUND_REJECTED);
        if (!validStatusesForForceRefund.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot force a refund on an order with status: " + order.getOrderStatus());
        }
        if (order.getPaymentDetails().getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot force refund a BANK_TRANSFER order. Please use the standard approval flow which requires a slip.");
        }
        return processRefund(order, null);
    }

    @Override
    @Transactional
    public OrderResponse updateRefundSlip(String orderId, MultipartFile newSlipImage) {
        if (newSlipImage == null || newSlipImage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new refund slip image is required.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        if (order.getOrderStatus() != OrderStatus.REFUNDED || order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot update refund slip for an order that is not in a refunded state via Bank Transfer.");
        }
        PaymentDetails details = order.getPaymentDetails();
        String oldRefundSlipUrl = details.getRefundSlipUrl();
        if (oldRefundSlipUrl != null && !oldRefundSlipUrl.isBlank()) {
            try {
                String oldKey = oldRefundSlipUrl.substring(oldRefundSlipUrl.lastIndexOf("/") + 1);
                s3Service.deleteFileByKey(oldKey);
            } catch (Exception e) {
                log.error("Failed to delete old refund slip for order {}: {}", orderId, e.getMessage());
            }
        }
        String newRefundSlipUrl = s3Service.uploadFile(newSlipImage);
        details.setRefundSlipUrl(newRefundSlipUrl);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse shipOrder(String orderId, ShipOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        List<OrderStatus> shippableStatuses = List.of(OrderStatus.PROCESSING, OrderStatus.RETURNED_TO_SENDER);
        if (!shippableStatuses.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be shipped. Current status is: " + order.getOrderStatus());
        }
        ShippingProvider provider = shippingProviderRepository.findByName(request.getShippingProvider())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping Provider not found: " + request.getShippingProvider()));
        ShippingDetails shippingDetails = ShippingDetails.builder()
                .shippingProvider(provider.getName())
                .shippingProviderLogoUrl(provider.getImageUrl())
                .trackingNumber(request.getTrackingNumber())
                .shippedAt(Instant.now())
                .build();
        order.setShippingDetails(shippingDetails);
        order.setOrderStatus(OrderStatus.SHIPPED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateShippingDetails(String orderId, ShipOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        if (order.getOrderStatus() != OrderStatus.SHIPPED && order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot update shipping details. The order has not been shipped yet. Current status: " + order.getOrderStatus());
        }
        if (order.getShippingDetails() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot update shipping details because they were never set in the first place.");
        }
        ShippingProvider provider = shippingProviderRepository.findByName(request.getShippingProvider())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping Provider not found: " + request.getShippingProvider()));
        ShippingDetails shippingDetails = order.getShippingDetails();
        shippingDetails.setShippingProvider(provider.getName());
        shippingDetails.setShippingProviderLogoUrl(provider.getImageUrl());
        shippingDetails.setTrackingNumber(request.getTrackingNumber());
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        OrderStatus currentStatus = order.getOrderStatus();
        List<OrderStatus> validTransitions = getValidManualTransitionsFor(currentStatus);
        if (!validTransitions.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status transition from '" + currentStatus + "' to '" + newStatus + "'.");
        }
        if (currentStatus == OrderStatus.PENDING_PAYMENT && newStatus == OrderStatus.CANCELLED && order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }
        order.setOrderStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return orderHelper.entityToResponse(order);
    }

    @Override
    public List<OrderStatus> getValidNextStatuses(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        return getValidManualTransitionsFor(order.getOrderStatus());
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
        return orderHelper.entityToResponse(order);
    }

    private CreateOrderResponse initiatePaypalPayment(Order order) throws PayPalRESTException {
        if (order.getId() == null) {
            orderRepository.save(order);
        }
        String formattedSuccessUrl = String.format(successUrl, order.getId());
        String formattedCancelUrl = String.format(cancelUrl, order.getId());
        Payment payment = paypalService.createPayment(order, "sale", "Order #" + order.getId(), formattedCancelUrl, formattedSuccessUrl);
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
        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .paypalOrderId(paypalPaymentId)
                .approvalLink(approvalLink)
                .build();
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not awaiting payment slip approval.");
        }
        updateOrderStatusToPaid(order);
        if (order.getPaymentDetails() != null) {
            PaymentDetails details = order.getPaymentDetails();
            details.setProviderStatus("APPROVED");
            details.setSlipRejectionReason(null);
        }
        orderRepository.save(order);
        log.info("Payment slip for order ID {} has been approved. Status updated to PROCESSING.", orderId);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse rejectPaymentSlip(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        if (order.getPaymentStatus() != PaymentStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reject slip. Order is not awaiting payment approval.");
        }
        order.setOrderStatus(OrderStatus.REJECTED_SLIP);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setUpdatedAt(Instant.now());
        if (order.getPaymentDetails() != null) {
            PaymentDetails details = order.getPaymentDetails();
            details.setProviderStatus("REJECTED");
            details.setSlipRejectionReason(reason);
        }
        orderRepository.save(order);
        return orderHelper.entityToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse revertSlipApproval(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        if (order.getOrderStatus() != OrderStatus.PROCESSING || order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot revert approval. Order is not in a valid state for this action.");
        }
        orderHelper.incrementStockForOrder(order);
        order.setOrderStatus(OrderStatus.REJECTED_SLIP);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setUpdatedAt(Instant.now());
        PaymentDetails details = order.getPaymentDetails();
        details.setProviderStatus("APPROVAL_REVERTED");
        details.setSlipRejectionReason(reason);
        orderRepository.save(order);
        return orderHelper.entityToResponse(order);
    }

    @Override
    public OrderResponse getAnyOrderByIdForAdmin(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        return orderHelper.entityToResponse(order);
    }

    private void updateOrderStatusToPaid(Order order) {
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setOrderStatus(OrderStatus.PROCESSING);
        order.setUpdatedAt(Instant.now());
    }

    private OrderResponse processRefund(Order order, MultipartFile refundSlip) throws PayPalRESTException {
        PaymentDetails paymentDetails = order.getPaymentDetails();
        if (paymentDetails == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment details are missing for this order.");
        }
        if (paymentDetails.getPaymentMethod() == PaymentMethod.PAYPAL) {
            orderHelper.processPaypalRefund(order, paymentDetails);
            paymentDetails.setProviderStatus("REFUNDED_VIA_PAYPAL");
        } else if (paymentDetails.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            if (refundSlip == null || refundSlip.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A refund slip is required for BANK_TRANSFER refunds.");
            }
            // THE FIX IS HERE:
            String refundSlipUrl = s3Service.uploadFile(refundSlip);
            paymentDetails.setRefundSlipUrl(refundSlipUrl);
            paymentDetails.setProviderStatus("MANUALLY_REFUNDED_COMPLETED");
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported payment method for refund.");
        }
        orderHelper.incrementStockForOrder(order);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        return orderHelper.entityToResponse(order);
    }

    private List<OrderStatus> getValidManualTransitionsFor(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING_PAYMENT, REJECTED_SLIP -> List.of(OrderStatus.CANCELLED);
            case SHIPPED, DELIVERY_FAILED -> Stream.of(OrderStatus.PROCESSING, OrderStatus.COMPLETED, OrderStatus.DELIVERY_FAILED, OrderStatus.RETURNED_TO_SENDER)
                    .filter(status -> status != currentStatus)
                    .toList();
            case RETURNED_TO_SENDER -> List.of(OrderStatus.PROCESSING);
            case REFUND_REJECTED -> List.of(OrderStatus.COMPLETED, OrderStatus.PROCESSING);
            default -> List.of();
        };
    }

    private Map<String, Integer> getRequiredStockForCartItem(String productId, LineItemType type, int quantity) {
        Map<String, Integer> requiredStock = new HashMap<>();
        if (type == LineItemType.COMPONENT) {
            requiredStock.put(productId, quantity);
        } else if (type == LineItemType.BUILD) {
            ComputerBuild build = buildRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found during stock calculation."));
            forEachComponentInBuild(build, (component, qtyInBuild) -> {
                int totalRequired = qtyInBuild * quantity;
                requiredStock.merge(component.getId(), totalRequired, Integer::sum);
            });
        }
        return requiredStock;
    }

    private void forEachComponentInBuild(ComputerBuild build, BiConsumer<Component, Integer> action) {
        Stream.of(build.getCpu(), build.getMotherboard(), build.getPsu(), build.getCaseDetail(), build.getCooler())
                .filter(Objects::nonNull)
                .forEach(component -> action.accept(component, 1));
        if (build.getRamKits() != null) {
            build.getRamKits().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
        if (build.getGpus() != null) {
            build.getGpus().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
        if (build.getStorageDrives() != null) {
            build.getStorageDrives().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
    }
}