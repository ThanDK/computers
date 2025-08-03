package in.project.computers.service.paypalService;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.entity.order.LineItemType;
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.OrderLineItem;
import in.project.computers.entity.order.OrderItemSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalServiceImpl implements PaypalService {

    private final APIContext apiContext;

    @Override
    public Payment createPayment(Order order, String intent, String description, String cancelUrl, String successUrl) throws PayPalRESTException {
        // === [CREATE-PAYMENT-1] สร้างรายการ Transaction จากข้อมูลออเดอร์ ===
        List<Transaction> transactions = getTransactions(order, description);

        // === [CREATE-PAYMENT-2] กำหนดข้อมูลผู้ชำระเงิน (Payer) และวิธีการชำระเงิน ===
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        // === [CREATE-PAYMENT-3] ประกอบอ็อบเจกต์ Payment หลัก ===
        Payment payment = new Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        // === [CREATE-PAYMENT-4] ตั้งค่า URL สำหรับ Redirect หลังจากทำรายการเสร็จสิ้น ===
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        // === [CREATE-PAYMENT-5] ส่งคำขอสร้าง Payment ไปยัง PayPal API ===
        log.info("Creating PayPal payment for order ID: {}", order.getId());
        return payment.create(apiContext);
    }

    /**
     * เมธอดภายในสำหรับสร้างรายการ Transaction โดยพยายามสร้างแบบมีรายละเอียดสินค้า (Itemized) ก่อน
     * หากล้มเหลว จะเปลี่ยนไปใช้แบบไม่มีรายละเอียด (Non-itemized) เพื่อให้การชำระเงินดำเนินต่อไปได้
     */
    private List<Transaction> getTransactions(Order order, String description) {
        try {
            // === [GET-TX-1] สร้างรายการสินค้า (Items) สำหรับ PayPal จาก OrderLineItem ===
            List<Item> paypalItems = new ArrayList<>();
            for (OrderLineItem lineItem : order.getLineItems()) {
                // === [GET-TX-1.1] กรณีเป็นสินค้าจัดสเปค (Build): แยกส่วนประกอบย่อยออกมาเป็น Item แต่ละชิ้น ===
                if (lineItem.getItemType() == LineItemType.BUILD && lineItem.getContainedItems() != null && !lineItem.getContainedItems().isEmpty()) {
                    for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                        Item item = new Item();
                        item.setName(part.getName())
                                .setQuantity(String.valueOf(part.getQuantity() * lineItem.getQuantity())) // คำนวณจำนวนรวม
                                .setPrice(part.getPriceAtTimeOfOrder().setScale(2, RoundingMode.HALF_UP).toString())
                                .setCurrency(order.getCurrency())
                                .setSku(part.getMpn());
                        paypalItems.add(item);
                    }
                } else {
                    // === [GET-TX-1.2] กรณีเป็นชิ้นส่วน (Component) ทั่วไป ===
                    Item item = new Item();
                    item.setName(lineItem.getName())
                            .setQuantity(String.valueOf(lineItem.getQuantity()))
                            .setPrice(lineItem.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toString())
                            .setCurrency(order.getCurrency())
                            .setSku(lineItem.getMpn() != null ? lineItem.getMpn() : lineItem.getBuildId());
                    paypalItems.add(item);
                }
            }

            // === [GET-TX-2] คำนวณยอดรวมย่อย (Subtotal) จากรายการสินค้าที่สร้างขึ้น ===
            BigDecimal calculatedSubtotal = paypalItems.stream()
                    .map(item -> new BigDecimal(item.getPrice()).multiply(new BigDecimal(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal tax = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;

            // === [GET-TX-3] ตรวจสอบความถูกต้องของยอดรวม (Validation) เพื่อป้องกันข้อผิดพลาดจาก PayPal ===
            BigDecimal expectedTotal = calculatedSubtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            BigDecimal actualTotal = order.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

            if (expectedTotal.compareTo(actualTotal) != 0) {
                log.error("PayPal itemization mismatch for order ID: {}. Itemized Total ({}) does not match Order Total ({}). Falling back to non-itemized transaction.",
                        order.getId(), expectedTotal, actualTotal);
                return createNonItemizedTransaction(order, description);
            }

            // === [GET-TX-4] สร้างอ็อบเจกต์ ItemList และ Amount พร้อม Details ===
            Details details = new Details();
            details.setShipping("0.00"); // กำหนดค่าส่งเป็น 0.00 เพื่อให้ยอดรวมถูกต้อง
            details.setSubtotal(calculatedSubtotal.setScale(2, RoundingMode.HALF_UP).toString());
            details.setTax(tax.setScale(2, RoundingMode.HALF_UP).toString());

            Amount amount = new Amount();
            amount.setCurrency(order.getCurrency());
            amount.setTotal(actualTotal.toString()); // ใช้ยอดรวมจากออเดอร์ที่ตรวจสอบแล้ว
            amount.setDetails(details);

            ItemList itemList = new ItemList();
            itemList.setItems(paypalItems);

            // === [GET-TX-5] เพิ่มข้อมูลที่อยู่สำหรับจัดส่ง (Shipping Address) เพื่อ Seller Protection ===
            if (order.getShippingAddress() != null) {
                // === [GET-TX-5.1] สร้างอ็อบเจกต์ ShippingAddress ของ PayPal จากข้อมูลในออเดอร์ ===
                in.project.computers.entity.user.Address ourAddress = order.getShippingAddress();
                ShippingAddress paypalShippingAddress = new ShippingAddress();

                paypalShippingAddress.setRecipientName(ourAddress.getContactName());
                paypalShippingAddress.setLine1(ourAddress.getLine1());
                if (ourAddress.getLine2() != null && !ourAddress.getLine2().isBlank()) {
                    paypalShippingAddress.setLine2(ourAddress.getLine2());
                }
                paypalShippingAddress.setCity(ourAddress.getDistrict());
                paypalShippingAddress.setState(ourAddress.getProvince());
                paypalShippingAddress.setPostalCode(ourAddress.getZipCode());
                paypalShippingAddress.setCountryCode(getCountryCode(ourAddress.getCountry()));

                // === [GET-TX-5.2] กำหนดที่อยู่ให้กับ ItemList เพื่อส่งให้ PayPal ===
                itemList.setShippingAddress(paypalShippingAddress);
                log.info("Shipping address for Order ID {} attached to PayPal payment.", order.getId());
            } else {
                // === [GET-TX-5.3] บันทึก Log เมื่อไม่มีที่อยู่ (เพื่อเตือนเรื่อง Seller Protection) ===
                log.warn("Order ID {} is proceeding to PayPal without a shipping address. Seller Protection might not apply.", order.getId());
            }

            // === [GET-TX-6] สร้าง Transaction แบบมีรายละเอียด (Itemized) เมื่อข้อมูลทั้งหมดถูกต้อง ===
            Transaction transaction = new Transaction();
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setItemList(itemList);

            return List.of(transaction);

        } catch (Exception e) {
            // === [GET-TX-7] จัดการข้อผิดพลาดที่ไม่คาดคิดและใช้ Fallback ===
            log.error("An unexpected error occurred while building the itemized PayPal transaction for order ID: {}. Falling back to non-itemized transaction. Error: {}", order.getId(), e.getMessage(), e);
            return createNonItemizedTransaction(order, description);
        }
    }

    /**
     * สร้าง Transaction แบบง่าย (ไม่มีรายละเอียดสินค้า) เพื่อเป็น Fallback
     * ทำให้การชำระเงินสามารถดำเนินต่อไปได้เสมอ แม้ว่าตรรกะการแจกแจงสินค้าจะล้มเหลว
     * @param order อ็อบเจกต์ Order เพื่อดึงยอดรวมและสกุลเงิน
     * @param description คำอธิบายสั้นๆ สำหรับ Transaction
     * @return List ที่มี Transaction แบบง่ายเพียง 1 รายการ
     */
    private List<Transaction> createNonItemizedTransaction(Order order, String description) {
        // === [FALLBACK-TX-1] สร้าง Amount แบบไม่มีรายละเอียดสินค้า มีเพียงยอดรวม ===
        Amount amount = new Amount();
        amount.setCurrency(order.getCurrency());
        amount.setTotal(order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString());

        // === [FALLBACK-TX-2] สร้าง Transaction แบบง่าย ===
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        return List.of(transaction);
    }

    @Override
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        // === [EXEC-PAYMENT-1] สร้างอ็อบเจกต์ Payment และระบุ ID ที่ได้จาก PayPal Callback ===
        Payment payment = new Payment();
        payment.setId(paymentId);

        // === [EXEC-PAYMENT-2] สร้างอ็อบเจกต์ PaymentExecution และระบุ Payer ID ===
        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);

        // === [EXEC-PAYMENT-3] ส่งคำขอยืนยันการชำระเงินไปยัง PayPal API ===
        log.info("Executing PayPal payment with PaymentID: {} and PayerID: {}", paymentId, payerId);
        return payment.execute(apiContext, paymentExecute);
    }

    @Override
    public Refund refundPayment(String saleId, BigDecimal amount, String currency) throws PayPalRESTException {
        // === [REFUND-1] สร้างอ็อบเจกต์ Sale และระบุ Sale ID ที่ต้องการจะคืนเงิน ===
        Sale sale = new Sale();
        sale.setId(saleId);

        // === [REFUND-2] สร้างอ็อบเจกต์ RefundRequest ===
        RefundRequest refundRequest = new RefundRequest();

        // === [REFUND-3] ตรวจสอบว่าเป็น Partial หรือ Full Refund และตั้งค่า Amount ตามนั้น ===
        if (amount != null) {
            // กรณีคืนเงินบางส่วน (Partial Refund)
            Amount refundAmount = new Amount();
            refundAmount.setCurrency(currency);
            refundAmount.setTotal(amount.setScale(2, RoundingMode.HALF_UP).toString());
            refundRequest.setAmount(refundAmount);
            log.info("Initiating PARTIAL refund for Sale ID: {}, Amount: {} {}", saleId, amount, currency);
        } else {
            // กรณีคืนเงินเต็มจำนวน (Full Refund) - ไม่ต้องระบุ Amount ใน Request
            log.info("Initiating FULL refund for Sale ID: {}", saleId);
        }

        try {
            // === [REFUND-4] ส่งคำขอ Refund ไปยัง PayPal API ===
            Refund refund = sale.refund(apiContext, refundRequest);
            log.info("PayPal refund API call successful for Sale ID: {}. Refund ID: {}, State: {}",
                    saleId, refund.getId(), refund.getState());
            return refund;
        } catch (PayPalRESTException e) {
            log.error("PayPalRESTException during refund for Sale ID: {}. Error: {}", saleId, e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage(), e);
            throw e;
        }
    }

    private String getCountryCode(String countryName) {
        // === [COUNTRY-CODE-1] ตั้งค่าเริ่มต้นเป็น "TH" หากไม่มีข้อมูลประเทศส่งมา ===
        if (countryName == null || countryName.isBlank()) {
            return "TH";
        }

        if (countryName.equalsIgnoreCase("thailand")) {
            return "TH";
        }
        if (countryName.equalsIgnoreCase("united states")) {
            return "US";
        }
        if (countryName.equalsIgnoreCase("singapore")) {
            return "SG";
        }

        // === [COUNTRY-CODE-3] ใช้ Locale เป็นทางเลือกสุดท้าย (Fallback) ===
        return new Locale.Builder().setRegion(countryName).build().getCountry();
    }
}