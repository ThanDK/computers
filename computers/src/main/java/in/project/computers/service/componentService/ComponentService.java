package in.project.computers.service.componentService;

import in.project.computers.DTO.component.componentRequest.ComponentRequest;
import in.project.computers.DTO.component.componentRequest.StockAdjustmentRequest;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public interface ComponentService {

    /**
     * สร้างชิ้นส่วนคอมพิวเตอร์ใหม่
     * @param request ข้อมูลชิ้นส่วนใหม่
     * @param imageFile ไฟล์รูปภาพ
     * @return ข้อมูลชิ้นส่วนที่สร้างใหม่
     * @throws ResponseStatusException หาก MPN ซ้ำ
     */
    ComponentResponse createComponent(ComponentRequest request, MultipartFile imageFile);

    /**
     * อัปเดตข้อมูลชิ้นส่วน
     * @param componentId ID ของชิ้นส่วน
     * @param request ข้อมูลใหม่
     * @param imageFile ไฟล์รูปภาพใหม่
     * @param removeImage true หากต้องการลบรูปภาพ
     * @return ข้อมูลชิ้นส่วนที่อัปเดตแล้ว
     * @throws ResponseStatusException หากไม่พบชิ้นส่วน
     */
    ComponentResponse updateComponent(String componentId, ComponentRequest request, MultipartFile imageFile, boolean removeImage);

    /**
     * ปรับสต็อกสินค้า
     * @param componentId ID ของชิ้นส่วน
     * @param request ข้อมูลจำนวนที่ต้องการปรับ
     * @return ข้อมูลชิ้นส่วนพร้อมสต็อกล่าสุด
     * @throws ResponseStatusException หากสต็อกไม่พอ
     */
    ComponentResponse adjustStock(String componentId, StockAdjustmentRequest request);

    /**
     * ลบชิ้นส่วน
     * @param componentId ID ของชิ้นส่วนที่จะลบ
     */
    void deleteComponent(String componentId);

    /**
     * ดึงข้อมูลชิ้นส่วนตาม ID
     * @param componentId ID ของชิ้นส่วน
     * @return ข้อมูลชิ้นส่วน
     * @throws ResponseStatusException หากไม่พบชิ้นส่วน
     */
    ComponentResponse getComponentDetailsById(String componentId);

    /**
     * ดึงรายการชิ้นส่วนทั้งหมด
     * @return รายการชิ้นส่วนทั้งหมด
     */
    List<ComponentResponse> getAllComponents();

}