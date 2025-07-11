package in.project.computers.service.PaypalService;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * <h3>PaypalService Implementation</h3>
 * <p>
 * คลาสที่ implement การทำงานของ {@link PaypalService} จริง
 * โดยใช้ PayPal SDK ในการสร้างและยืนยันการชำระเงิน
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalServiceImpl implements PaypalService {

    private final APIContext apiContext;

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment createPayment(BigDecimal total, String currency, String intent, String description, String cancelUrl, String successUrl) throws PayPalRESTException {
        // 1. สร้างอ็อบเจกต์ Amount สำหรับระบุยอดเงินและสกุลเงิน
        List<Transaction> transactions = getTransactions(total, currency, description);

        // 3. สร้างอ็อบเจกต์ Payer เพื่อระบุว่าผู้จ่ายจะจ่ายด้วยวิธี "paypal"
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        // 4. ประกอบทุกอย่างเข้าด้วยกันเป็นอ็อบเจกต์ Payment หลัก
        Payment payment = new Payment();
        // ** FIX HERE: เพิ่มการตั้งค่า intent ซึ่งเป็น field ที่จำเป็น **
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        // 5. สร้างอ็อบเจกต์ RedirectUrls เพื่อตั้งค่า URL สำหรับ redirect กลับมา
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        // 6. ส่งคำร้องขอสร้าง Payment ไปยัง PayPal API และ return ผลลัพธ์ที่ได้กลับไป
        return payment.create(apiContext);
    }

    private static List<Transaction> getTransactions(BigDecimal total, String currency, String description) {
        Amount amount = new Amount();
        amount.setCurrency(currency);
        // PayPal ต้องการยอดเงินเป็น String ที่มีทศนิยม 2 ตำแหน่งเสมอ
        amount.setTotal(total.setScale(2, RoundingMode.HALF_UP).toString());

        // 2. สร้างอ็อบเจกต์ Transaction เพื่ออธิบายรายการที่จะเกิดขึ้น
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);
        return transactions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        // 1. สร้างอ็อบเจกต์ Payment ว่างๆ โดยระบุ ID ของ Payment ที่ต้องการจะยืนยัน
        Payment payment = new Payment();
        payment.setId(paymentId);

        // 2. สร้างอ็อบเจกต์ PaymentExecution สำหรับยืนยันการจ่ายเงิน โดยใช้ PayerID ที่ได้มา
        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);

        // 3. ส่งคำร้องขอยืนยันการจ่ายเงินไปยัง PayPal API และ return ผลลัพธ์ที่ได้กลับไป
        return payment.execute(apiContext, paymentExecute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Refund refundPayment(String saleId, BigDecimal amount, String currency) throws PayPalRESTException {
        // 1. สร้างอ็อบเจกต์ Sale โดยระบุ ID ของ Sale ที่ต้องการจะ Refund
        //    Sale ID นี้คือ Payment ID ที่เราได้มาตอน executePayment สำเร็จ
        Sale sale = new Sale();
        sale.setId(saleId);

        // 2. สร้างอ็อบเจกต์ RefundRequest เพื่อกำหนดรายละเอียดการคืนเงิน
        RefundRequest refundRequest = new RefundRequest();

        // 3. ตรวจสอบว่าต้องการคืนเงินเต็มจำนวนหรือบางส่วน
        if (amount != null) {
            // ถ้ามีการระบุยอดเงิน
            Amount refundAmount = new Amount();
            refundAmount.setCurrency(currency);
            // PayPal ต้องการยอดเงินเป็น String ที่มีทศนิยม 2 ตำแหน่ง
            refundAmount.setTotal(amount.setScale(2, RoundingMode.HALF_UP).toString());
            refundRequest.setAmount(refundAmount);
            log.info("Initiating PARTIAL refund for Sale ID: {}, Amount: {} {}", saleId, amount, currency);

        } else {
            // ถ้า amount เป็น null คือคืนเงินเต็มจำนวน (Full Refund)
            // ไม่ต้อง setAmount ใน refundRequest, PayPal SDK จะจัดการให้เอง
            log.info("Initiating FULL refund for Sale ID: {}", saleId);
        }

        // 4. ส่งคำร้องขอ Refund ไปยัง PayPal API
        try {
            Refund refund = sale.refund(apiContext, refundRequest);
            log.info("PayPal refund API call successful for Sale ID: {}. Refund ID: {}, State: {}",
                    saleId, refund.getId(), refund.getState());
            return refund;

        } catch (PayPalRESTException e) {

            log.error("PayPalRESTException during refund for Sale ID: {}. Error: {}", saleId, e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage(), e);
            throw e; // โยน Exception ต่อไปเพื่อให้ Service ชั้นบนจัดการ
        }
    }
}