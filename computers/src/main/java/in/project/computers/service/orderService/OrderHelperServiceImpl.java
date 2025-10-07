package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.api.payments.Sale;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.address.AddressRequest;
import in.project.computers.DTO.address.AddressResponse;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.DTO.order.orderResponse.PaymentDetailsResponse;
import in.project.computers.entity.component.Component;
import in.project.computers.entity.component.Inventory;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.Address;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.componentRepository.ComponentRepository;
import in.project.computers.repository.componentRepository.InventoryRepository;
import in.project.computers.service.addressService.AddressConverter;
import in.project.computers.service.addressService.AddressService;
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
import java.util.stream.Collectors;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
public class OrderHelperServiceImpl implements OrderHelperService {

    private final ComponentRepository componentRepository;
    private final InventoryRepository inventoryRepository;
    private final PaypalService paypalService;
    private final APIContext apiContext;
    private final AddressService addressService;
    private final AddressConverter addressConverter;

    @Value("${app.currency:THB}")
    private String currency;

    @Value("${app.tax-rate:0.00}")
    private BigDecimal taxRate;
    @Override
    public Order createAndValidateOrderFromCart(Cart cart, CreateOrderRequest request, UserEntity currentUser) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be created from an empty cart.");
        }
        Address shippingAddress = resolveShippingAddress(request, currentUser);

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
            } else {
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
                .shippingAddress(shippingAddress)
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

        log.info("Structured order from cart created for user: {}. Address: {}, Subtotal: {}, Tax: {}, Total: {} {}",
                currentUser.getEmail(), shippingAddress.getLine1(), subtotal, taxAmount, totalAmount, this.currency);
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

        if (requiredStock.isEmpty()) {
            return;
        }

        List<String> componentIds = new ArrayList<>(requiredStock.keySet());
        Map<String, Integer> availableStockMap = inventoryRepository.findByComponentIdIn(componentIds).stream()
                .collect(Collectors.toMap(Inventory::getComponentId, Inventory::getQuantity));
        Map<String, String> componentNameMap = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(Component::getId, Component::getName));

        for (Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
            String componentId = entry.getKey();
            int required = entry.getValue();
            int availableStock = availableStockMap.getOrDefault(componentId, 0);

            if (availableStock < required) {
                String componentName = componentNameMap.getOrDefault(componentId, componentId);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for: " + componentName + ". Please remove it from your cart or reduce the quantity.");
            }
        }
    }

    @Override
    public void incrementStockForOrder(Order order) {
        Map<String, Integer> stockChanges = new HashMap<>();
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                stockChanges.merge(lineItem.getComponentId(), lineItem.getQuantity(), Integer::sum);
            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    int totalQuantityToAdd = part.getQuantity() * lineItem.getQuantity();
                    stockChanges.merge(part.getComponentId(), totalQuantityToAdd, Integer::sum);
                }
            }
        }

        if (!stockChanges.isEmpty()) {
            inventoryRepository.bulkAtomicUpdateQuantities(stockChanges);
            log.info("Stock successfully incremented for order ID: {}", order.getId());
        }
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

    private Address resolveShippingAddress(CreateOrderRequest request, UserEntity user) {
        if (request.getSavedAddressId() != null && !request.getSavedAddressId().isBlank()) {
            log.info("Resolving address using savedAddressId: {}", request.getSavedAddressId());
            AddressResponse savedAddressResponse = addressService.getAddressById(user.getId(), request.getSavedAddressId());
            return addressConverter.convertResponseToEntity(savedAddressResponse);
        } else if (request.getNewAddress() != null) {
            log.info("Resolving address using newAddress object.");
            AddressRequest newAddrRequest = request.getNewAddress();
            return addressConverter.convertRequestToEntity(newAddrRequest);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A shipping address is required. Please provide either a savedAddressId or a newAddress object.");
        }
    }

    @Override
    public OrderResponse entityToResponse(Order order) {
        PaymentDetailsResponse paymentDetailsResponse = null;
        if (order.getPaymentDetails() != null) {
            PaymentDetails detailsEntity = order.getPaymentDetails();
            paymentDetailsResponse = PaymentDetailsResponse.builder()
                    .paymentMethod(detailsEntity.getPaymentMethod())
                    .transactionId(detailsEntity.getTransactionId())
                    .providerStatus(detailsEntity.getProviderStatus())
                    .slipImageUrl(detailsEntity.getSlipImageUrl())
                    .slipRejectionReason(detailsEntity.getSlipRejectionReason())
                    .payerId(detailsEntity.getPayerId())
                    .payerEmail(detailsEntity.getPayerEmail())
                    .refundSlipUrl(detailsEntity.getRefundSlipUrl())
                    .build();
        }

        AddressResponse shippingAddressResponse = addressConverter.convertEntityToResponse(order.getShippingAddress());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .shippingAddress(shippingAddressResponse)
                .email(order.getEmail())
                .lineItems(order.getLineItems())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .taxAmount(order.getTaxAmount())
                .orderStatus(order.getOrderStatus())
                .shippingDetails(order.getShippingDetails())
                .paymentStatus(order.getPaymentStatus())
                .paymentDetails(paymentDetailsResponse)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}