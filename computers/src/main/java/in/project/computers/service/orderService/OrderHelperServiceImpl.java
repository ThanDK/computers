package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.api.payments.Sale;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.entity.component.*;

import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.ComponentRepo.ComponentRepository;
import in.project.computers.repository.ComponentRepo.InventoryRepository;
import in.project.computers.repository.generalRepo.ComputerBuildRepository;
import in.project.computers.repository.generalRepo.OrderRepository;
import in.project.computers.service.PaypalService.PaypalService;
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
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// คลาสผู้ช่วยสำหรับจัดการ Logic ที่ซับซ้อนของ Order
@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
public class OrderHelperServiceImpl implements OrderHelperService {

    // --- Dependencies ---
    private final ComponentRepository componentRepository;
    private final InventoryRepository inventoryRepository;
    private final ComputerBuildRepository buildRepository;
    private final OrderRepository orderRepository;
    private final PaypalService paypalService;
    private final APIContext apiContext;

    // --- Config Properties ---

    @Value("${app.currency:THB}")
    private String currency;

    // --- [NEW] Inject Tax Rate from properties file ---
    // ดึงค่า tax rate จาก application.properties
    @Value("${app.tax-rate:0.00}")
    private BigDecimal taxRate;

    @Override
    public Order createAndValidateBaseOrder(CreateOrderRequest request, UserEntity currentUser) {
        if ((request.getBuildItems() == null || request.getBuildItems().isEmpty()) &&
                (request.getComponentItems() == null || request.getComponentItems().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be empty.");
        }

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        Map<String, Component> allNeededComponents = fetchAllRequiredComponents(request);
        Map<String, Inventory> allNeededInventories = fetchAllRequiredInventories(allNeededComponents.keySet());

        // --- Calculate Subtotal from all line items ---
        if (request.getBuildItems() != null && !request.getBuildItems().isEmpty()) {
            List<ComputerBuild> builds = buildRepository.findAllById(request.getBuildItems().keySet());
            for (ComputerBuild build : builds) {
                int quantityOrdered = request.getBuildItems().get(build.getId());
                OrderLineItem buildLineItem = createBuildLineItem(build, quantityOrdered, allNeededInventories);
                lineItems.add(buildLineItem);
                subtotal = subtotal.add(buildLineItem.getUnitPrice().multiply(BigDecimal.valueOf(quantityOrdered)));
            }
        }
        if (request.getComponentItems() != null && !request.getComponentItems().isEmpty()) {
            for (Map.Entry<String, Integer> entry : request.getComponentItems().entrySet()) {
                String componentId = entry.getKey();
                int quantity = entry.getValue();
                OrderLineItem componentLineItem = createComponentLineItem(componentId, quantity, allNeededComponents, allNeededInventories);
                lineItems.add(componentLineItem);
                subtotal = subtotal.add(componentLineItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            }
        }

        validateOverallStock(request, allNeededInventories);


        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        // The total amount is the sum of subtotal and tax, with no shipping cost.
        BigDecimal totalAmount = subtotal.add(taxAmount);

        // --- Build the final Order object ---
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

        log.info("Structured base order created for user: {}. Subtotal: {}, Tax: {}, Total: {} {}",
                currentUser.getEmail(), subtotal, taxAmount, totalAmount, this.currency);
        return order;
    }

    // --- Private Helper Methods for Order Creation ---

    /**
     * สร้าง OrderLineItem สำหรับสินค้าประเภทชุดประกอบ (Build)
     */
    private OrderLineItem createBuildLineItem(ComputerBuild build, int quantity, Map<String, Inventory> inventoryMap) {
        // === [HELPER-BUILD-LI-1] เตรียม List สำหรับเก็บ Snapshot ของชิ้นส่วนภายใน Build ===
        List<OrderItemSnapshot> snapshots = new ArrayList<>();

        // === [HELPER-BUILD-LI-2] วนลูปทุกชิ้นส่วนใน Build เพื่อสร้าง Snapshot ===
        forEachComponentInBuild(build, (component, qty) -> {
            Inventory inventory = inventoryMap.get(component.getId());
            if (inventory == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Inventory missing for " + component.getName());
            // สร้าง Snapshot ที่บันทึกข้อมูลและราคา ณ เวลาที่สั่งซื้อ
            snapshots.add(new OrderItemSnapshot(component.getId(), component.getName(), component.getMpn(), qty, inventory.getPrice()));
        });

        // === [HELPER-BUILD-LI-3] คำนวณราคารวมของ Build จากราคา Snapshot ของแต่ละชิ้นส่วน ===
        BigDecimal buildPrice = snapshots.stream()
                .map(s -> s.getPriceAtTimeOfOrder().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // === [HELPER-BUILD-LI-4] สร้างและคืนค่า OrderLineItem สำหรับ Build ===
        return OrderLineItem.builder()
                .itemType(LineItemType.BUILD).name(build.getBuildName()).quantity(quantity)
                .unitPrice(buildPrice).buildId(build.getId()).containedItems(snapshots)
                .build();
    }

    /**
     * สร้าง OrderLineItem สำหรับสินค้าประเภทชิ้นส่วนเดี่ยว (Component)
     */
    private OrderLineItem createComponentLineItem(String componentId, int quantity, Map<String, Component> componentMap, Map<String, Inventory> inventoryMap) {
        // === [HELPER-COMP-LI-1] ดึงข้อมูล Component และ Inventory จาก Map ที่เตรียมไว้ ===
        Component component = componentMap.get(componentId);
        Inventory inventory = inventoryMap.get(componentId);
        if (component == null || inventory == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Standalone component with ID " + componentId + " could not be found.");
        }

        // === [HELPER-COMP-LI-2] สร้างและคืนค่า OrderLineItem ===
        return OrderLineItem.builder()
                .itemType(LineItemType.COMPONENT).name(component.getName()).quantity(quantity)
                .unitPrice(inventory.getPrice()).componentId(component.getId()).mpn(component.getMpn())
                .build();
    }

    @Override
    public void decrementStockForOrder(Order order) {
        // === [HELPER-STOCK-DEC-1] วนลูปทุกรายการในออเดอร์ ===
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                // === [HELPER-STOCK-DEC-2] กรณีเป็นชิ้นส่วนเดี่ยว: ลดสต็อกตามจำนวนที่สั่ง ===
                updateStock(lineItem.getComponentId(), -lineItem.getQuantity());
            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                // === [HELPER-STOCK-DEC-3] กรณีเป็นชุดประกอบ: วนลูปชิ้นส่วนภายใน ===
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    // คำนวณจำนวนที่ต้องลดทั้งหมด = (จำนวนชิ้นส่วนใน 1 build * จำนวน build ที่สั่ง)
                    int totalQuantityToRemove = part.getQuantity() * lineItem.getQuantity();
                    updateStock(part.getComponentId(), -totalQuantityToRemove);
                }
            }
        }
        log.info("Stock successfully decremented for order ID: {}", order.getId());
    }

    @Override
    public void incrementStockForOrder(Order order) {
        // === [HELPER-STOCK-INC-1] วนลูปทุกรายการในออเดอร์ ===
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                // === [HELPER-STOCK-INC-2] กรณีเป็นชิ้นส่วนเดี่ยว: เพิ่มสต็อกตามจำนวนที่สั่ง ===
                updateStock(lineItem.getComponentId(), lineItem.getQuantity());
            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                // === [HELPER-STOCK-INC-3] กรณีเป็นชุดประกอบ: วนลูปชิ้นส่วนภายใน ===
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    // คำนวณจำนวนที่ต้องเพิ่มทั้งหมด = (จำนวนชิ้นส่วนใน 1 build * จำนวน build ที่สั่ง)
                    int totalQuantityToAdd = part.getQuantity() * lineItem.getQuantity();
                    updateStock(part.getComponentId(), totalQuantityToAdd);
                }
            }
        }
        log.info("Stock successfully incremented for order ID: {}", order.getId());
    }

    /**
     * เมธอดกลางสำหรับอัปเดตสต็อกสินค้า (ทั้งเพิ่มและลด) และสถานะ Active ของ Component
     */
    private void updateStock(String componentId, int quantityChange) {
        // === [HELPER-STOCK-UPDATE-1] ค้นหา Inventory ของสินค้า ===
        Inventory inventory = inventoryRepository.findByComponentId(componentId)
                .orElseThrow(() -> new IllegalStateException("Data Inconsistency: Inventory not found for component ID " + componentId));

        // === [HELPER-STOCK-UPDATE-2] คำนวณสต็อกใหม่และตรวจสอบ ===
        int newQuantity = inventory.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            // กรณีนี้ไม่ควรเกิดถ้า validateOverallStock ทำงานถูกต้อง แต่ใส่ไว้เพื่อป้องกัน
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock for component ID " + componentId + " was depleted.");
        }
        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        // === [HELPER-STOCK-UPDATE-3] อัปเดตสถานะ Active ของ Component ตามสต็อก ===
        // ถ้าสต็อก > 0 -> Active, ถ้า = 0 -> Inactive
        componentRepository.findById(componentId).ifPresent(component -> {
            boolean shouldBeActive = newQuantity > 0;
            if (component.isActive() != shouldBeActive) {
                component.setActive(shouldBeActive);
                componentRepository.save(component);
            }
        });
    }

    /**
     * ตรวจสอบสต็อกโดยรวมของสินค้าทั้งหมดในตะกร้า
     */
    private void validateOverallStock(CreateOrderRequest request, Map<String, Inventory> inventoryMap) {
        // === [HELPER-VALIDATE-1] สร้าง Map เพื่อรวบรวมจำนวนสินค้าทั้งหมดที่ต้องการ ===
        Map<String, Integer> requiredStock = new HashMap<>();
        // === [HELPER-VALIDATE-2] รวบรวมจากชุดประกอบ ===
        if (request.getBuildItems() != null && !request.getBuildItems().isEmpty()) {
            List<ComputerBuild> builds = buildRepository.findAllById(request.getBuildItems().keySet());
            for (ComputerBuild build : builds) {
                int buildQty = request.getBuildItems().get(build.getId());
                forEachComponentInBuild(build, (component, qty) ->
                        requiredStock.merge(component.getId(), qty * buildQty, Integer::sum)
                );
            }
        }
        // === [HELPER-VALIDATE-3] รวบรวมจากชิ้นส่วนเดี่ยว ===
        if (request.getComponentItems() != null) {
            request.getComponentItems().forEach((id, qty) -> requiredStock.merge(id, qty, Integer::sum));
        }
        // === [HELPER-VALIDATE-4] วนลูปตรวจสอบสต็อกที่รวบรวมได้กับสต็อกจริง ===
        for (Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
            String componentId = entry.getKey();
            int required = entry.getValue();
            Inventory inventory = inventoryMap.get(componentId);
            if (inventory == null || inventory.getQuantity() < required) {
                // ถ้าสต็อกไม่พอ ให้โยน Exception
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for component ID " + componentId);
            }
        }
    }

    // --- Optimization Helper Methods ---

    /**
     * รวบรวม ID ของ Component ทั้งหมดที่ต้องการจาก Request
     */
    private Map<String, Component> fetchAllRequiredComponents(CreateOrderRequest request) {
        List<String> componentIds = new ArrayList<>();
        if (request.getComponentItems() != null) {
            componentIds.addAll(request.getComponentItems().keySet());
        }
        if (request.getBuildItems() != null && !request.getBuildItems().isEmpty()) {
            buildRepository.findAllById(request.getBuildItems().keySet()).forEach(build ->
                    forEachComponentInBuild(build, (component, qty) -> componentIds.add(component.getId()))
            );
        }
        return componentRepository.findAllById(componentIds).stream().collect(Collectors.toMap(Component::getId, Function.identity()));
    }

    /**
     * ดึงข้อมูล Inventory ทั้งหมดตามรายการ ID ของ Component
     */
    private Map<String, Inventory> fetchAllRequiredInventories(Iterable<String> componentIds) {
        List<String> idList = new ArrayList<>();
        componentIds.forEach(idList::add);
        return inventoryRepository.findAllByComponentIdIn(idList).stream()
                .collect(Collectors.toMap(Inventory::getComponentId, Function.identity()));
    }

    /**
     * วนลูปทุกชิ้นส่วนภายใน ComputerBuild เพื่อทำ Action บางอย่าง
     */
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

    @Override
    public void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException {
        // === [HELPER-REFUND-1] ตรวจสอบว่ามี Transaction ID เดิมของ PayPal หรือไม่ ===
        if (paymentDetails.getTransactionId() == null || paymentDetails.getTransactionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Original PayPal Transaction ID not found for this order.");
        }
        // === [HELPER-REFUND-2] ดึงข้อมูล Payment เดิมจาก PayPal ===
        Payment originalPayment = Payment.get(this.apiContext, paymentDetails.getTransactionId());
        // === [HELPER-REFUND-3] ดึง Sale ID ซึ่งจำเป็นต่อการ Refund ===
        String saleId = extractSaleIdFromPaypalPayment(originalPayment, order.getId());
        // === [HELPER-REFUND-4] เรียกใช้ service เพื่อทำการ Refund ===
        Refund refund = paypalService.refundPayment(saleId, null, order.getCurrency());
        // === [HELPER-REFUND-5] ตรวจสอบผลลัพธ์และอัปเดตข้อมูล ===
        if ("completed".equalsIgnoreCase(refund.getState()) || "pending".equalsIgnoreCase(refund.getState())) {
            paymentDetails.setProviderStatus(refund.getState());
            paymentDetails.setTransactionId(refund.getId()); // อัปเดต Transaction ID เป็น ID ของการ Refund
        } else {
            // === [HELPER-REFUND-6] กรณี Refund ล้มเหลว ===
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PayPal refund failed. State: " + refund.getState());
        }
    }

    @Override
    public String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog) {
        // === [HELPER-SALEID-1] ตรวจสอบโครงสร้างของ Payment object ===
        if (originalPaypalPayment == null || originalPaypalPayment.getTransactions() == null || originalPaypalPayment.getTransactions().isEmpty() ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources() == null ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID: Invalid PayPal payment structure.");
        }
        // === [HELPER-SALEID-2] ดึง Sale object ออกมา ===
        Sale sale = originalPaypalPayment.getTransactions().getFirst().getRelatedResources().getFirst().getSale();
        if (sale == null || sale.getId() == null || sale.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID from PayPal payment's related resources.");
        }
        // === [HELPER-SALEID-3] คืนค่า Sale ID ===
        return sale.getId();
    }



    @Override
    public Order findOrderForProcessing(String orderId, String userId, PaymentMethod expectedMethod) {
        // === [HELPER-FIND-1] ค้นหาออเดอร์ ===
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        // === [HELPER-FIND-2] ตรวจสอบความเป็นเจ้าของ ===
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order.");
        }
        // === [HELPER-FIND-3] ตรวจสอบวิธีการชำระเงินที่คาดหวัง ===
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != expectedMethod) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect payment method for this action.");
        }
        // === [HELPER-FIND-4] ตรวจสอบสถานะการชำระเงิน ===
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not pending payment.");
        }
        return order;
    }

    @Override
    public OrderResponse entityToResponse(Order order) {
        // === [HELPER-MAP-1] แปลง Entity -> DTO โดยใช้ Builder Pattern ===
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