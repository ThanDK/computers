package in.project.computers.service.componentService;

import in.project.computers.DTO.component.componentRequest.ComponentRequest;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import in.project.computers.entity.component.Component;

public interface ComponentConverter {
    /**
     * อัปเดตข้อมูล Entity จาก Request DTO
     * @param entityToUpdate Entity ที่จะอัปเดต
     * @param request DTO ที่มีข้อมูลใหม่
     */
    void updateEntityFromRequest(Component entityToUpdate, ComponentRequest request);

    /**
     * แปลง Request DTO เป็น Entity
     * @param request DTO ที่มีข้อมูล
     * @return Entity ใหม่ที่สร้างขึ้น
     */
    Component convertRequestToEntity(ComponentRequest request);

    /**
     * แปลง Entity เป็น Response DTO
     * @param entity Entity จากฐานข้อมูล
     * @return DTO กลาง
     */
    ComponentResponse convertEntityToResponse(Component entity);

    /**
     * แปลง Entity เป็น Response DTO ที่ระบุชนิด
     * @param entity Entity จากฐานข้อมูล
     * @param responseClass Class ของ Response DTO ที่ต้องการ
     * @param <T> ชนิดของ Component Entity
     * @param <R> ชนิดของ ComponentResponse DTO
     * @return Instance ของ Response DTO
     */
    <T extends Component, R extends ComponentResponse> R convertEntityToResponse(T entity, Class<R> responseClass);
}