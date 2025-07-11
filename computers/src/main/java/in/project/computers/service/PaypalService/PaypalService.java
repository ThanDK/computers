package in.project.computers.service.PaypalService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.base.rest.PayPalRESTException;
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

    /**
     * สร้างรายการชำระเงิน (Payment) บนระบบของ PayPal
     * <p>
     * เมธอดนี้จะส่งข้อมูลที่จำเป็นทั้งหมดไปยัง PayPal เพื่อสร้าง session การชำระเงิน
     * และจะคืนค่าเป็นอ็อบเจกต์ Payment ซึ่งมี `approval_link` ที่เราต้องใช้
     * เพื่อ redirect ผู้ใช้ไปหน้าชำระเงินของ PayPal
     * </p>
     *
     * @param total       ยอดเงินรวมที่ต้องชำระ (BigDecimal)
     * @param currency    สกุลเงิน (เช่น "THB", "USD")
     * @param intent      เจตนาของการทำรายการ ในที่นี้คือ "sale" สำหรับการขายสินค้า
     * @param description คำอธิบายรายการสำหรับแสดงผลในหน้า PayPal (เช่น "Order #123")
     * @param cancelUrl   URL ที่จะให้ PayPal redirect กลับมาหากผู้ใช้กดยกเลิกการชำระเงิน
     * @param successUrl  URL ที่จะให้ PayPal redirect กลับมาหากผู้ใช้ชำระเงินสำเร็จ (Callback URL)
     * @return อ็อบเจกต์ {@link Payment} ที่สร้างโดย PayPal ซึ่งจะมี approval_link อยู่ข้างใน
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อกับ PayPal API
     */
    Payment createPayment(
            BigDecimal total,
            String currency,
            String intent,
            String description,
            String cancelUrl,
            String successUrl
    ) throws PayPalRESTException;

    /**
     * ยืนยันการชำระเงิน (Execute Payment) หลังจากที่ผู้ใช้กดอนุมัติในหน้าเว็บ PayPal แล้ว
     * <p>
     * เมื่อผู้ใช้ชำระเงินสำเร็จ PayPal จะ redirect กลับมาที่ `successUrl` พร้อมกับ `paymentId` และ `PayerID`
     * เราต้องใช้ข้อมูลนี้เพื่อเรียกเมธอดนี้และยืนยันกับ PayPal ว่าการทำรายการเสร็จสมบูรณ์
     * </p>
     *
     * @param paymentId ID ของ Payment ที่ได้จากขั้นตอน createPayment และถูกส่งกลับมาใน Callback URL
     * @param payerId   ID ของผู้ชำระเงินที่ PayPal ส่งกลับมาพร้อมกับ Callback URL
     * @return อ็อบเจกต์ {@link Payment} ที่มีสถานะ "approved" หากการยืนยันสำเร็จ
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการยืนยันการชำระเงินกับ PayPal
     */
    Payment executePayment(String paymentId, String payerId) throws PayPalRESTException;

    /**
     * ทำการคืนเงิน (Refund) สำหรับ Transaction การขาย (Sale) ที่เกิดขึ้นบน PayPal
     *
     * @param saleId ID ของ Sale transaction ที่ต้องการคืนเงิน
     *               (โดยทั่วไปคือ PayPal Payment ID ที่ได้จากการ executePayment สำเร็จ)
     * @param amount ยอดเงินที่ต้องการคืน หากเป็น `null` จะหมายถึงการคืนเงินเต็มจำนวน (Full Refund)
     *               หากระบุยอดเงิน จะเป็นการคืนเงินบางส่วน (Partial Refund)
     * @param currency สกุลเงินของยอดที่ต้องการคืน (ต้องตรงกับสกุลเงินของ Transaction เดิม)
     * @return อ็อบเจกต์ {@link Refund} ที่มีรายละเอียดการคืนเงินจาก PayPal
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อหรือดำเนินการกับ PayPal API
     */
    Refund refundPayment(String saleId, BigDecimal amount, String currency) throws PayPalRESTException;


}