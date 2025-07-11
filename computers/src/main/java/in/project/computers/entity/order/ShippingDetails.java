package in.project.computers.entity.order;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * <h3>Shipping Details Entity</h3>
 * <p>
 * คลาสสำหรับเก็บข้อมูลที่เกี่ยวข้องกับการจัดส่งทั้งหมดไว้ด้วยกันอย่างเป็นระเบียบ
 * ถูกใช้เป็น Embedded Document ภายใน {@link Order}
 * </p>
 */
@Data
@Builder
public class ShippingDetails {

    /**
     * บริษัทขนส่ง เช่น "Kerry Express", "Flash Express", "ไปรษณีย์ไทย"
     */
    private String shippingProvider;

    /**
     * หมายเลขพัสดุสำหรับติดตาม (Tracking Number)
     */
    private String trackingNumber;

    /**
     * วันและเวลาที่ทำการจัดส่ง
     */
    private Instant shippedAt;
}