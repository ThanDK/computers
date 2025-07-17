package in.project.computers.service.PaypalService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.entity.order.Order;

import java.math.BigDecimal;

/**
 * <h3>PaypalService Interface</h3>
 * <p>
 * กำหนดเมธอดมาตรฐานสำหรับการติดต่อกับ PayPal API
 * เพื่อแยก Logic การจ่ายเงินออกจาก Business Logic หลักของแอปพลิเคชัน
 * ทำให้โค้ดสะอาดและง่ายต่อการทดสอบหรือเปลี่ยนแปลงในอนาคต
 * </p>
 */
public interface PaypalService {

    Payment createPayment(Order order, String intent, String description, String cancelUrl, String successUrl) throws PayPalRESTException;

    Payment executePayment(String paymentId, String payerId) throws PayPalRESTException;

    Refund refundPayment(String saleId, BigDecimal amount, String currency) throws PayPalRESTException;
}