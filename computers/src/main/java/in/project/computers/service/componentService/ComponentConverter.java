package in.project.computers.service.componentService;

import in.project.computers.dto.component.componentRequest.ComponentRequest;
import in.project.computers.dto.component.componentResponse.ComponentResponse;
import in.project.computers.entity.component.Component;

/**
 * เอกสารอธิบาย:
 * Interface สำหรับ Component Converter
 * ได้เพิ่มเมธอดใหม่ที่ปลอดภัยต่อชนิดข้อมูล (Type-Safe) คือ convertEntityToResponse(Component, Class<R>)
 * เพื่อกำจัดคำเตือน "Unchecked Cast" ในระบบ
 */
public interface ComponentConverter {
    void updateEntityFromRequest(Component entityToUpdate, ComponentRequest request);

    Component convertRequestToEntity(ComponentRequest request);

    /**
     * เมธอดดั้งเดิม: แปลง Entity เป็น Response ทั่วไป
     * @param entity Entity จากฐานข้อมูล
     * @return ComponentResponse ทั่วไป
     */
    ComponentResponse convertEntityToResponse(Component entity);

    /**
     * เมธอดใหม่ (Type-Safe): แปลง Entity เป็น Response ชนิดที่ระบุ และทำการ cast อย่างปลอดภัย
     * @param entity Entity จากฐานข้อมูล
     * @param responseClass Class ของ Response DTO ที่ต้องการ (เช่น CpuResponse.class)
     * @param <T> ชนิดของ Component Entity
     * @param <R> ชนิดของ ComponentResponse DTO
     * @return instance ของ Response DTO ที่ระบุ
     */
    <T extends Component, R extends ComponentResponse> R convertEntityToResponse(T entity, Class<R> responseClass);
}