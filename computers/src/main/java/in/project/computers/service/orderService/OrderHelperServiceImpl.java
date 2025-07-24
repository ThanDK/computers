package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.api.payments.Sale;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.entity.component.*;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.ComponentRepo.ComponentRepository;
import in.project.computers.repository.ComponentRepo.InventoryRepository;
import in.project.computers.repository.generalRepo.OrderRepository;

import in.project.computers.service.paypalService.PaypalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
public class OrderHelperServiceImpl implements OrderHelperService {

    private final ComponentRepository componentRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final PaypalService paypalService;
    private final APIContext apiContext;

    @Value("${app.currency:THB}")
    private String currency;

    @Value("${app.tax-rate:0.00}")
    private BigDecimal taxRate;

    /**
     * @deprecated Use createAndValidateOrderFromCart instead. This method works with the old DTO.
     */
    @Override
    @Deprecated
    public Order createAndValidateBaseOrder(CreateOrderRequest request, UserEntity currentUser) {
        throw new UnsupportedOperationException("This method is deprecated. Use createAndValidateOrderFromCart instead.");
    }

    @Override
    public Order createAndValidateOrderFromCart(Cart cart, CreateOrderRequest request, UserEntity currentUser) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be created from an empty cart.");
        }

        validateOverallStockFromCart(cart);

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            OrderLineItem lineItem;
            if (cartItem.getItemType() == LineItemType.BUILD) {
                lineItem = OrderLineItem.builder()
                        .itemType(LineItemType.BUILD)
                        .name(cartItem.getName())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getUnitPrice())
                        .buildId(cartItem.getProductId())
                        .containedItems(cartItem.getContainedItemsSnapshot())
                        .imageUrl(null)
                        .build();
            } else { // COMPONENT
                Component component = componentRepository.findById(cartItem.getProductId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component with ID " + cartItem.getProductId() + " not found."));

                lineItem = OrderLineItem.builder()
                        .itemType(LineItemType.COMPONENT)
                        .name(cartItem.getName())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getUnitPrice())
                        .imageUrl(cartItem.getImageUrl())
                        .componentId(cartItem.getProductId())
                        .mpn(component.getMpn())
                        .build();
            }
            lineItems.add(lineItem);
            subtotal = subtotal.add(lineItem.getUnitPrice().multiply(BigDecimal.valueOf(lineItem.getQuantity())));
        }

        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        Order order = Order.builder()
                .userId(currentUser.getId())
                .userAddress(request.getUserAddress())
                .phoneNumber(request.getPhoneNumber())
                .email(currentUser.getEmail())
                .lineItems(lineItems)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .currency(this.currency)
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        log.info("Structured order from cart created for user: {}. Subtotal: {}, Tax: {}, Total: {} {}",
                currentUser.getEmail(), subtotal, taxAmount, totalAmount, this.currency);
        return order;
    }

    private void validateOverallStockFromCart(Cart cart) {
        Map<String, Integer> requiredStock = new HashMap<>();
        for (CartItem item : cart.getItems()) {
            if (item.getItemType() == LineItemType.BUILD) {

                for (OrderItemSnapshot part : item.getContainedItemsSnapshot()) {
                    int totalRequiredForBuild = part.getQuantity() * item.getQuantity();
                    requiredStock.merge(part.getComponentId(), totalRequiredForBuild, Integer::sum);
                }
            } else if (item.getItemType() == LineItemType.COMPONENT) {
                requiredStock.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
            String componentId = entry.getKey();
            int required = entry.getValue();
            int availableStock = inventoryRepository.findByComponentId(componentId)
                    .map(Inventory::getQuantity)
                    .orElse(0);

            if (availableStock < required) {
                String componentName = componentRepository.findById(componentId).map(Component::getName).orElse(componentId);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for: " + componentName + ". Please remove it from your cart or reduce the quantity.");
            }
        }
    }


    @Override
    public void decrementStockForOrder(Order order) {
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                updateStock(lineItem.getComponentId(), -lineItem.getQuantity());
            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    int totalQuantityToRemove = part.getQuantity() * lineItem.getQuantity();
                    updateStock(part.getComponentId(), -totalQuantityToRemove);
                }
            }
        }
        log.info("Stock successfully decremented for order ID: {}", order.getId());
    }

    @Override
    public void incrementStockForOrder(Order order) {
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                updateStock(lineItem.getComponentId(), lineItem.getQuantity());
            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    int totalQuantityToAdd = part.getQuantity() * lineItem.getQuantity();
                    updateStock(part.getComponentId(), totalQuantityToAdd);
                }
            }
        }
        log.info("Stock successfully incremented for order ID: {}", order.getId());
    }

    private void updateStock(String componentId, int quantityChange) {
        Inventory inventory = inventoryRepository.findByComponentId(componentId)
                .orElseThrow(() -> new IllegalStateException("Data Inconsistency: Inventory not found for component ID " + componentId));

        int newQuantity = inventory.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock for component ID " + componentId + " was depleted.");
        }
        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        componentRepository.findById(componentId).ifPresent(component -> {
            boolean shouldBeActive = newQuantity > 0;
            if (component.isActive() != shouldBeActive) {
                component.setActive(shouldBeActive);
                componentRepository.save(component);
            }
        });
    }

    @Override
    public void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException {
        if (paymentDetails.getTransactionId() == null || paymentDetails.getTransactionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Original PayPal Transaction ID not found for this order.");
        }
        Payment originalPayment = Payment.get(this.apiContext, paymentDetails.getTransactionId());
        String saleId = extractSaleIdFromPaypalPayment(originalPayment, order.getId());
        Refund refund = paypalService.refundPayment(saleId, null, order.getCurrency());
        if ("completed".equalsIgnoreCase(refund.getState()) || "pending".equalsIgnoreCase(refund.getState())) {
            paymentDetails.setProviderStatus(refund.getState());
            paymentDetails.setTransactionId(refund.getId());
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PayPal refund failed. State: " + refund.getState());
        }
    }

    @Override
    public String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog) {
        if (originalPaypalPayment == null || originalPaypalPayment.getTransactions() == null || originalPaypalPayment.getTransactions().isEmpty() ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources() == null ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID: Invalid PayPal payment structure.");
        }
        Sale sale = originalPaypalPayment.getTransactions().getFirst().getRelatedResources().getFirst().getSale();
        if (sale == null || sale.getId() == null || sale.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID from PayPal payment's related resources.");
        }
        return sale.getId();
    }

    @Override
    public Order findOrderForProcessing(String orderId, String userId, PaymentMethod expectedMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order.");
        }
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != expectedMethod) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect payment method for this action.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not pending payment.");
        }
        return order;
    }

    @Override
    public OrderResponse entityToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userAddress(order.getUserAddress())
                .phoneNumber(order.getPhoneNumber())
                .email(order.getEmail())
                .lineItems(order.getLineItems())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .taxAmount(order.getTaxAmount())
                .orderStatus(order.getOrderStatus())
                .shippingDetails(order.getShippingDetails())
                .paymentStatus(order.getPaymentStatus())
                .paymentDetails(order.getPaymentDetails())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}