package in.project.computers.service.componentCompatibility;


import in.project.computers.DTO.builds.CompatibilityCheckRequest;
import in.project.computers.DTO.builds.CompatibilityResult;

/**
 * Interface หลักสำหรับบริการตรวจสอบความเข้ากันได้ของชุดคอมพิวเตอร์
 * <p>
 * ทำหน้าที่เป็นตัวกลางในการเรียกใช้การตรวจสอบต่างๆ จาก {@link CompatibilityHelper}
 * เพื่อสรุปผลความเข้ากันได้ของบิลด์ทั้งหมด
 */
public interface ComponentCompatibilityService {

    /**
     * ตรวจสอบความเข้ากันได้ของส่วนประกอบทั้งหมดในบิลด์ที่ระบุ
     *
     * @param buildId ID ของบิลด์ที่ต้องการตรวจสอบ
     * @return อ็อบเจ็กต์ {@link CompatibilityResult} ที่มีรายการข้อผิดพลาด (errors),
     *         คำเตือน (warnings), และค่า Wattage ที่คำนวณได้
     */
    CompatibilityResult checkCompatibility(String buildId);

    CompatibilityResult checkCompatibility(CompatibilityCheckRequest request);
}