package in.project.computers.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String id;
    private String contactName; // ชื่อ-นามสกุลผู้รับ
    private String phoneNumber; // เบอร์โทรศัพท์
    private String line1; // บ้านเลขที่, หมู่, ซอย, ถนน
    private String line2; // อาคาร, ชั้น (ถ้ามี) (Optional)
    private String subdistrict; // ตำบล / แขวง
    private String district; // อำเภอ / เขต
    private String province; // จังหวัด
    private String zipCode; // รหัสไปรษณีย์

    @Builder.Default
    private String country = "Thailand";

    private boolean isDefault;
}