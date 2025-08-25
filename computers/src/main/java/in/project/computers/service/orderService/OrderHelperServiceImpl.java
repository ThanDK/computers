package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.api.payments.Sale;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.address.AddressDTO;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.DTO.order.orderResponse.PaymentDetailsResponse;
import in.project.computers.entity.component.*;
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
        // === [CREATE-3.1] ตรวจสอบว่าตะกร้าสินค้าไม่ว่างเปล่า ===
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be created from an empty cart.");
        }
        // === [CREATE-3.2] ดึงข้อมูลที่อยู่สำหรับจัดส่ง ===
        Address shippingAddress = resolveShippingAddress(request, currentUser);

        // === [CREATE-3.3] ตรวจสอบสต็อกสินค้าทั้งหมดที่ต้องการในตะกร้าก่อนสร้างออเดอร์ ===
        validateOverallStockFromCart(cart);

        // === [CREATE-3.4] สร้างรายการสินค้า (LineItems) จากตะกร้า และคำนวณยอดรวมย่อย ===
        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            OrderLineItem lineItem;
            if (cartItem.getItemType() == LineItemType.BUILD) {
                // === [CREATE-3.4.1] กรณีเป็นสินค้าจัดสเปค (Build) ===
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
                // === [CREATE-3.4.2] กรณีเป็นชิ้นส่วน (Component) ===
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

        // === [CREATE-3.5] คำนวณภาษีและยอดรวมสุทธิ ===
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        // === [CREATE-3.6] สร้างอ็อบเจกต์ Order พร้อมข้อมูลทั้งหมด (UPDATED) ===
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

    /**
     * เมธอดภายในสำหรับตรวจสอบสต็อกสินค้าทั้งหมดในตะกร้า
     */
    private void validateOverallStockFromCart(Cart cart) {
        // === [CREATE-3.3.1] รวบรวมจำนวนชิ้นส่วนทั้งหมดที่ต้องการจากทุกรายการในตะกร้า ===
        Map<String, Integer> requiredStock = new HashMap<>();
        for (CartItem item : cart.getItems()) {
            if (item.getItemType() == LineItemType.BUILD) {
                // หากเป็น Build, วนลูปในชิ้นส่วนย่อย
                for (OrderItemSnapshot part : item.getContainedItemsSnapshot()) {
                    int totalRequiredForBuild = part.getQuantity() * item.getQuantity();
                    requiredStock.merge(part.getComponentId(), totalRequiredForBuild, Integer::sum);
                }
            } else if (item.getItemType() == LineItemType.COMPONENT) {
                // หากเป็น Component, เพิ่มจำนวนที่ต้องการโดยตรง
                requiredStock.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        // === [CREATE-3.3.2] ตรวจสอบสต็อกคงเหลือในคลังกับจำนวนที่ต้องการ ===
        if (requiredStock.isEmpty()) {
            return;
        }

        // === [CREATE-3.3.3] ดึงข้อมูลสต็อกและชื่อของ Component ที่เกี่ยวข้อง ===
        List<String> componentIds = new ArrayList<>(requiredStock.keySet());
        Map<String, Integer> availableStockMap = inventoryRepository.findByComponentIdIn(componentIds).stream()
                .collect(Collectors.toMap(Inventory::getComponentId, Inventory::getQuantity));
        Map<String, String> componentNameMap = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(Component::getId, Component::getName));

        // === [CREATE-3.3.4] เปรียบเทียบสต็อกคงเหลือกับจำนวนที่ต้องการสำหรับแต่ละชิ้นส่วน ===
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
    public void decrementStockForOrder(Order order) {
        // === [PPC-4.1] / [APPROVE-SLIP-3.1] รวบรวมจำนวนสต็อกที่จะตัดทั้งหมด ===
        Map<String, Integer> stockChanges = new HashMap<>();
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                stockChanges.merge(lineItem.getComponentId(), -lineItem.getQuantity(), Integer::sum);

            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    int totalQuantityToRemove = part.getQuantity() * lineItem.getQuantity();
                    stockChanges.merge(part.getComponentId(), -totalQuantityToRemove, Integer::sum);
                }
            }
        }

        // === [PPC-4.2] / [APPROVE-SLIP-3.2] ส่งคำสั่งตัดสต็อกทั้งหมดในครั้งเดียว ===
        if (!stockChanges.isEmpty()) {
            inventoryRepository.bulkAtomicUpdateQuantities(stockChanges);
            log.info("Stock successfully decremented for order ID: {}", order.getId());
        }
    }



    @Override
    public void incrementStockForOrder(Order order) {
        // === [PROCESS-REFUND-3.1] / [REVERT-2.1] รวบรวมจำนวนสต็อกที่จะคืนทั้งหมด ===
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

        // === [PROCESS-REFUND-3.2] / [REVERT-2.2] ส่งคำสั่งคืนสต็อกทั้งหมดในครั้งเดียว ===
        if (!stockChanges.isEmpty()) {
            inventoryRepository.bulkAtomicUpdateQuantities(stockChanges);
            log.info("Stock successfully incremented for order ID: {}", order.getId());
        }
    }

    @Override
    public void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException {
        // === [PROCESS-REFUND-2.1] ตรวจสอบว่ามี Transaction ID เดิมของ PayPal หรือไม่ ===
        if (paymentDetails.getTransactionId() == null || paymentDetails.getTransactionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Original PayPal Transaction ID not found for this order.");
        }
        // === [PROCESS-REFUND-2.2] ดึงข้อมูล Payment เดิมจาก PayPal ===
        Payment originalPayment = Payment.get(this.apiContext, paymentDetails.getTransactionId());
        // === [PROCESS-REFUND-2.3] ดึง Sale ID จาก Payment เพื่อใช้ในการคืนเงิน ===
        String saleId = extractSaleIdFromPaypalPayment(originalPayment, order.getId());
        // === [PROCESS-REFUND-2.4] เรียกใช้ PaypalService เพื่อดำเนินการคืนเงิน ===
        Refund refund = paypalService.refundPayment(saleId, null, order.getCurrency());
        // === [PROCESS-REFUND-2.5] อัปเดตข้อมูลใน PaymentDetails ตามผลลัพธ์จาก PayPal ===
        if ("completed".equalsIgnoreCase(refund.getState()) || "pending".equalsIgnoreCase(refund.getState())) {
            paymentDetails.setProviderStatus(refund.getState());
            paymentDetails.setTransactionId(refund.getId()); // อัปเดต Transaction ID เป็น ID ของการ Refund
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PayPal refund failed. State: " + refund.getState());
        }
    }

    @Override
    public String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog) {
        // === [PROCESS-REFUND-2.3.1] ตรวจสอบโครงสร้างของอ็อบเจกต์ Payment ที่ได้รับจาก PayPal ===
        if (originalPaypalPayment == null || originalPaypalPayment.getTransactions() == null || originalPaypalPayment.getTransactions().isEmpty() ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources() == null ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID: Invalid PayPal payment structure.");
        }
        // === [PROCESS-REFUND-2.3.2] ดึงข้อมูล Sale จาก Related Resources ===
        Sale sale = originalPaypalPayment.getTransactions().getFirst().getRelatedResources().getFirst().getSale();
        if (sale == null || sale.getId() == null || sale.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID from PayPal payment's related resources.");
        }
        return sale.getId();
    }

    private Address resolveShippingAddress(CreateOrderRequest request, UserEntity user) {
        // === [CREATE-3.2.1] กรณีใช้ที่อยู่ที่บันทึกไว้ ===
        if (request.getSavedAddressId() != null && !request.getSavedAddressId().isBlank()) {
            log.info("Resolving address using savedAddressId: {}", request.getSavedAddressId());
            AddressDTO savedAddressDto = addressService.getAddressById(user.getId(), request.getSavedAddressId());
            return addressConverter.convertDtoToEntity(savedAddressDto);
            // === [CREATE-3.2.2] กรณีใช้ที่อยู่ใหม่ที่กรอกเข้ามา ===
        } else if (request.getNewAddress() != null) {
            log.info("Resolving address using newAddress object.");
            AddressDTO newAddrDTO = request.getNewAddress();
            return addressConverter.convertDtoToEntity(newAddrDTO);
            // === [CREATE-3.2.3] กรณีไม่ระบุที่อยู่ ===
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A shipping address is required. Please provide either a savedAddressId or a newAddress object.");
        }
    }

    @Override
    public OrderResponse entityToResponse(Order order) {
        // === [RESPONSE-MAPPER-1] แปลง PaymentDetails entity เป็น PaymentDetailsResponse DTO (ถ้ามี) ===
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
                    .build();
        }

        // === [RESPONSE-MAPPER-2] แปลง Address entity เป็น AddressDTO ===
        AddressDTO shippingAddressDto = addressConverter.convertEntityToDto(order.getShippingAddress());

        // === [RESPONSE-MAPPER-3] สร้าง OrderResponse DTO หลักและประกอบข้อมูลทั้งหมด ===
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .shippingAddress(shippingAddressDto)
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