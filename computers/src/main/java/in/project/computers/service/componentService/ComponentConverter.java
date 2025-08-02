package in.project.computers.service.componentService;

import in.project.computers.DTO.component.componentRequest.ComponentRequest;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import in.project.computers.entity.component.Component;

/**
 * Interface สำหรับบริการแปลงข้อมูล (Converter) ที่รับผิดชอบการแปลงอ็อบเจกต์ระหว่างชั้นต่างๆ
 * ทำหน้าที่เป็นสะพานเชื่อมระหว่าง Data Transfer Objects (DTOs) ที่ใช้ใน API และ Entities ที่ใช้ในฐานข้อมูล
 */
public interface ComponentConverter {
    /**
     * อัปเดตข้อมูลใน Entity ที่มีอยู่แล้วจากข้อมูลใน Request DTO
     * <p>
     * ไม่ได้สร้างอ็อบเจกต์ใหม่ แต่จะแก้ไขค่าใน {@code entityToUpdate} โดยตรง
     * </p>
     * @param entityToUpdate Entity ที่ดึงมาจากฐานข้อมูลและต้องการจะอัปเดต
     * @param request DTO ที่มีข้อมูลใหม่
     */
    void updateEntityFromRequest(Component entityToUpdate, ComponentRequest request);

    /**
     * แปลง Request DTO เป็น Entity ใหม่ทั้งหมด
     * <p>
     * ใช้ในกระบวนการสร้างชิ้นส่วนใหม่ (Create)
     * </p>
     * @param request DTO จาก Client
     * @return Entity ใหม่ที่พร้อมสำหรับบันทึกลงฐานข้อมูล
     */
    Component convertRequestToEntity(ComponentRequest request);

    /**
     * แปลง Entity เป็น Response DTO แบบพื้นฐาน (ไม่ระบุชนิดเจาะจง)
     * @param entity Entity จากฐานข้อมูล
     * @return {@link ComponentResponse} ซึ่งเป็น DTO กลาง
     */
    ComponentResponse convertEntityToResponse(Component entity);

    /**
     * แปลง Entity เป็น Response DTO ชนิดที่ระบุอย่างปลอดภัย (Type-Safe)
     * <p>
     * เมธอดนี้ถูกออกแบบมาเพื่อหลีกเลี่ยงการ Cast ที่ไม่ปลอดภัย (Unchecked Cast) ในโค้ดที่เรียกใช้,
     * ทำให้โค้ดมีความเสถียรและอ่านง่ายขึ้น
     * </p>
     * @param entity Entity จากฐานข้อมูล
     * @param responseClass Class ของ Response DTO ที่ต้องการ (เช่น {@code CpuResponse.class})
     * @param <T> ชนิดของ Component Entity (เช่น {@code Cpu})
     * @param <R> ชนิดของ ComponentResponse DTO (เช่น {@code CpuResponse})
     * @return Instance ของ Response DTO ที่ระบุ
     */
    <T extends Component, R extends ComponentResponse> R convertEntityToResponse(T entity, Class<R> responseClass);
}