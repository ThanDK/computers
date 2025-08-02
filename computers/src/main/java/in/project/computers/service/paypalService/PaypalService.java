package in.project.computers.service.paypalService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.entity.order.Order;

import java.math.BigDecimal;

/**
 * Interface ที่กำหนดสัญญา (Contract) สำหรับการติดต่อกับ PayPal API
 * <p>
 * ทำหน้าที่เป็น Abstraction Layer เพื่อแยก Logic การชำระเงินออกจาก Business Logic หลักของแอปพลิเคชัน
 * ทำให้โค้ดสะอาด, ง่ายต่อการทดสอบ, และสามารถสลับไปใช้ผู้ให้บริการชำระเงินรายอื่นได้ในอนาคต
 * </p>
 */
public interface PaypalService {

    /**
     * สร้างรายการชำระเงิน (Payment) บนระบบของ PayPal
     * <p>
     * เป็นขั้นตอนแรกของกระบวนการชำระเงิน โดยจะส่งข้อมูลออเดอร์ไปให้ PayPal และรับลิงก์สำหรับให้ผู้ใช้ไปอนุมัติการชำระเงินกลับมา
     * </p>
     *
     * @param order      อ็อบเจกต์ Order ของเราที่มีข้อมูลยอดรวมและรายการสินค้า
     * @param intent     เจตนาของการชำระเงิน (โดยทั่วไปคือ "sale")
     * @param description คำอธิบายสั้นๆ สำหรับการทำรายการ
     * @param cancelUrl  URL ที่จะให้ PayPal redirect กลับมาหากผู้ใช้ยกเลิก
     * @param successUrl URL ที่จะให้ PayPal redirect กลับมาหากผู้ใช้ทำรายการสำเร็จ
     * @return อ็อบเจกต์ {@link Payment} จาก PayPal SDK ซึ่งมี `approval_url`
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อกับ API ของ PayPal
     */
    Payment createPayment(Order order, String intent, String description, String cancelUrl, String successUrl) throws PayPalRESTException;

    /**
     * ยืนยันการชำระเงิน (Execute Payment) หลังจากที่ผู้ใช้ทำรายการบนเว็บ PayPal สำเร็จ
     * <p>
     * เป็นขั้นตอนที่สอง โดยใช้ `paymentId` และ `payerId` ที่ได้จาก Callback URL เพื่อยืนยันว่าการชำระเงินได้รับการอนุมัติแล้ว
     * </p>
     *
     * @param paymentId ID ของการชำระเงินที่ได้จาก PayPal
     * @param payerId   ID ของผู้ชำระเงินที่ได้จาก PayPal
     * @return อ็อบเจกต์ {@link Payment} ที่มีสถานะอัปเดตเป็น "approved"
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการยืนยันการชำระเงิน
     */
    Payment executePayment(String paymentId, String payerId) throws PayPalRESTException;

    /**
     * ดำเนินการคืนเงิน (Refund) สำหรับรายการขายที่เคยชำระเงินไปแล้ว
     *
     * @param saleId   ID ของการขาย (Sale) ที่ต้องการคืนเงิน (ได้จาก Transaction เดิม)
     * @param amount   จำนวนเงินที่ต้องการคืน (หากเป็น `null` จะเป็นการคืนเงินเต็มจำนวน)
     * @param currency สกุลเงิน
     * @return อ็อบเจกต์ {@link Refund} ที่มีสถานะของการคืนเงิน
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในกระบวนการคืนเงิน
     */
    Refund refundPayment(String saleId, BigDecimal amount, String currency) throws PayPalRESTException;
}