package in.project.computers.entity.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// For MongoDB, this class would be embedded in the Order document.

/**
 * คลาสสำหรับเก็บรายละเอียดการชำระเงิน
 * **แก้ไขแล้ว:** เพิ่มฟิลด์เฉพาะสำหรับ Bank Transfer ตามที่ต้องการ เพื่อแยกข้อมูลออกจาก PayPal อย่างชัดเจน
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails {


    private PaymentMethod paymentMethod;

    private String transactionId;


    private String slipImageUrl;


    private String slipRejectionReason;


    private String providerStatus;

    private String payerId;


    private String payerEmail;
}