package in.project.computers.service.componentService;

import in.project.computers.DTO.component.componentRequest.ComponentRequest;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import in.project.computers.entity.component.Component;

/**
 * Interface สำหรับบริการที่รับผิดชอบในการแปลงข้อมูลระหว่าง Component Entity และ Data Transfer Objects (DTOs).
 * <p>
 * ทำหน้าที่เป็นสะพานเชื่อม เพื่อแยกชั้นของข้อมูลที่ใช้ในฐานข้อมูล (Entity) ออกจากข้อมูลที่ใช้ในการสื่อสารผ่าน API (DTO).
 */
public interface ComponentConverter {
    /**
     * อัปเดตข้อมูลใน Entity ที่มีอยู่แล้วจากข้อมูลใน Request DTO.
     * <p>
     * เมธอดนี้ไม่ได้สร้างอ็อบเจกต์ใหม่ แต่จะแก้ไขค่าใน {@code entityToUpdate} โดยตรง
     * เพื่อใช้ในกระบวนการอัปเดต (Update)
     * </p>
     * @param entityToUpdate Entity ที่ดึงมาจากฐานข้อมูลและต้องการจะอัปเดต
     * @param request DTO ที่มีข้อมูลใหม่จาก Client
     */
    void updateEntityFromRequest(Component entityToUpdate, ComponentRequest request);

    /**
     * แปลง Request DTO เป็น Entity ใหม่ทั้งหมด.
     * <p>
     * ใช้ในกระบวนการสร้างชิ้นส่วนใหม่ (Create) โดยจะสร้าง instance ใหม่ของ Entity
     * ที่พร้อมสำหรับบันทึกลงฐานข้อมูล
     * </p>
     * @param request DTO ที่มีข้อมูลมาจาก Client
     * @return Entity ใหม่ที่ถูกสร้างขึ้น
     */
    Component convertRequestToEntity(ComponentRequest request);

    /**
     * แปลง Entity เป็น Response DTO แบบพื้นฐาน ({@link ComponentResponse}) โดยไม่ระบุชนิดย่อยที่เจาะจง.
     * <p>
     * เหมาะสำหรับกรณีที่ต้องการข้อมูลกลางๆ โดยไม่จำเป็นต้องทราบว่าเป็น CPU, GPU, ฯลฯ
     * เช่น ในรายการสินค้าทั้งหมด
     * </p>
     * @param entity Entity จากฐานข้อมูล
     * @return {@link ComponentResponse} ซึ่งเป็น DTO กลาง
     */
    ComponentResponse convertEntityToResponse(Component entity);

    /**
     * แปลง Entity เป็น Response DTO ชนิดที่เจาะจงและปลอดภัย (Type-Safe).
     * <p>
     * เมธอดนี้ช่วยให้ผู้เรียกสามารถระบุ Class ของ DTO ที่ต้องการได้โดยตรง (เช่น {@code CpuResponse.class})
     * ทำให้โค้ดที่ได้มีความเสถียร, อ่านง่าย, และหลีกเลี่ยงการ Cast ที่ไม่ปลอดภัย (Unchecked Cast).
     * </p>
     * @param entity Entity จากฐานข้อมูล (เช่น instance ของ {@code Cpu})
     * @param responseClass Class ของ Response DTO ที่ต้องการ (เช่น {@code CpuResponse.class})
     * @param <T> ชนิดของ Component Entity (เช่น {@code Cpu})
     * @param <R> ชนิดของ ComponentResponse DTO (เช่น {@code CpuResponse})
     * @return Instance ของ Response DTO ที่ระบุ
     */
    <T extends Component, R extends ComponentResponse> R convertEntityToResponse(T entity, Class<R> responseClass);
}