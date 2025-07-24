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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalServiceImpl implements PaypalService {

    private final APIContext apiContext;

    @Override
    public Payment createPayment(Order order, String intent, String description, String cancelUrl, String successUrl) throws PayPalRESTException {

        List<Transaction> transactions = getTransactions(order, description);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        return payment.create(apiContext);
    }


    private List<Transaction> getTransactions(Order order, String description) {
        try {

            List<Item> paypalItems = new ArrayList<>();
            for (OrderLineItem lineItem : order.getLineItems()) {
                if (lineItem.getItemType() == LineItemType.BUILD && lineItem.getContainedItems() != null && !lineItem.getContainedItems().isEmpty()) {
                    for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                        Item item = new Item();
                        item.setName(part.getName())
                                .setQuantity(String.valueOf(part.getQuantity() * lineItem.getQuantity()))
                                .setPrice(part.getPriceAtTimeOfOrder().setScale(2, RoundingMode.HALF_UP).toString())
                                .setCurrency(order.getCurrency())
                                .setSku(part.getMpn());
                        paypalItems.add(item);
                    }
                } else {
                    Item item = new Item();
                    item.setName(lineItem.getName())
                            .setQuantity(String.valueOf(lineItem.getQuantity()))
                            .setPrice(lineItem.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toString())
                            .setCurrency(order.getCurrency())
                            .setSku(lineItem.getMpn() != null ? lineItem.getMpn() : lineItem.getBuildId());
                    paypalItems.add(item);
                }
            }


            BigDecimal calculatedSubtotal = paypalItems.stream()
                    .map(item -> new BigDecimal(item.getPrice()).multiply(new BigDecimal(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal tax = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;


            BigDecimal expectedTotal = calculatedSubtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            BigDecimal actualTotal = order.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

            if (expectedTotal.compareTo(actualTotal) != 0) {
                log.error("PayPal itemization mismatch for order ID: {}. Itemized Total ({}) does not match Order Total ({}). " +
                                "Falling back to non-itemized transaction.",
                        order.getId(), expectedTotal, actualTotal);
                return createNonItemizedTransaction(order, description);
            }

            // 4. Build the detailed transaction, with shipping explicitly set to zero
            Details details = new Details();
            details.setShipping("0.00"); // Explicitly set shipping to 0.
            details.setSubtotal(calculatedSubtotal.setScale(2, RoundingMode.HALF_UP).toString());
            details.setTax(tax.setScale(2, RoundingMode.HALF_UP).toString());

            Amount amount = new Amount();
            amount.setCurrency(order.getCurrency());
            amount.setTotal(actualTotal.toString()); // Use the validated total
            amount.setDetails(details);

            ItemList itemList = new ItemList();
            itemList.setItems(paypalItems);

            Transaction transaction = new Transaction();
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setItemList(itemList);

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);
            return transactions;
        } catch (Exception e) {
            log.error("An unexpected error occurred while building the itemized PayPal transaction for order ID: {}. " +
                    "Falling back to non-itemized transaction. Error: {}", order.getId(), e.getMessage(), e);
            return createNonItemizedTransaction(order, description);
        }
    }

    /**
     * Creates a simple, non-itemized transaction as a robust fallback.
     * This ensures payment can always proceed even if the complex itemization logic fails.
     * @param order The order object to get total amount and currency from.
     * @param description A short description for the transaction.
     * @return A list containing one simple transaction object.
     */
    private List<Transaction> createNonItemizedTransaction(Order order, String description) {
        Amount amount = new Amount();
        amount.setCurrency(order.getCurrency());
        amount.setTotal(order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString());

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);
        return transactions;
    }

    @Override
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);

        return payment.execute(apiContext, paymentExecute);
    }

    @Override
    public Refund refundPayment(String saleId, BigDecimal amount, String currency) throws PayPalRESTException {
        Sale sale = new Sale();
        sale.setId(saleId);

        RefundRequest refundRequest = new RefundRequest();

        if (amount != null) {
            Amount refundAmount = new Amount();
            refundAmount.setCurrency(currency);
            refundAmount.setTotal(amount.setScale(2, RoundingMode.HALF_UP).toString());
            refundRequest.setAmount(refundAmount);
            log.info("Initiating PARTIAL refund for Sale ID: {}, Amount: {} {}", saleId, amount, currency);
        } else {
            log.info("Initiating FULL refund for Sale ID: {}", saleId);
        }

        try {
            Refund refund = sale.refund(apiContext, refundRequest);
            log.info("PayPal refund API call successful for Sale ID: {}. Refund ID: {}, State: {}",
                    saleId, refund.getId(), refund.getState());
            return refund;
        } catch (PayPalRESTException e) {
            log.error("PayPalRESTException during refund for Sale ID: {}. Error: {}", saleId, e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage(), e);
            throw e;
        }
    }
}