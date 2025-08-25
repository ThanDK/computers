package in.project.computers.service.componentService;

import in.project.computers.DTO.component.componentRequest.ComponentRequest;
import in.project.computers.DTO.component.componentRequest.StockAdjustmentRequest;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Interface ที่กำหนดสัญญา (Contract) สำหรับบริการจัดการชิ้นส่วนคอมพิวเตอร์ (Component) ทั้งหมดในระบบ
 * <p>
 * เป็น Service หลักที่รวบรวมตรรกะทางธุรกิจ (Business Logic) ที่เกี่ยวข้องกับการสร้าง, แก้ไข, ลบ,
 * และค้นหาข้อมูลชิ้นส่วนคอมพิวเตอร์
 */
public interface ComponentService {

    /**
     * สร้างชิ้นส่วนคอมพิวเตอร์ใหม่พร้อมกับข้อมูลในคลัง (Inventory).
     * <p>
     * รับผิดชอบในการตรวจสอบข้อมูลซ้ำซ้อน (เช่น MPN), อัปโหลดรูปภาพไปยัง S3,
     * และบันทึกข้อมูลทั้งหมดลงฐานข้อมูล
     * </p>
     * @param request DTO ที่มีข้อมูลทั้งหมดของชิ้นส่วนใหม่ เช่น ชื่อ, MPN, ราคา, และจำนวนเริ่มต้น
     * @param imageFile ไฟล์รูปภาพของชิ้นส่วน (เป็นทางเลือก, อาจเป็น null ได้)
     * @return {@link ComponentResponse} ที่มีข้อมูลของชิ้นส่วนที่ถูกสร้างขึ้นใหม่
     * @throws ResponseStatusException หากมี MPN (รหัสผู้ผลิต) ซ้ำในระบบ
     */
    ComponentResponse createComponent(ComponentRequest request, MultipartFile imageFile);

    /**
     * อัปเดตข้อมูลของชิ้นส่วนที่มีอยู่แล้วในระบบ.
     * <p>
     * สามารถอัปเดตได้ทั้งข้อมูลทั่วไป, ราคา, และรูปภาพ (โดยการอัปโหลดไฟล์ใหม่, ลบไฟล์เดิม, หรือไม่เปลี่ยนแปลง)
     * </p>
     * @param componentId ID ของชิ้นส่วนที่ต้องการอัปเดต
     * @param request DTO ที่มีข้อมูลใหม่
     * @param imageFile ไฟล์รูปภาพใหม่ที่ต้องการอัปโหลด (เป็น null หากไม่ต้องการเปลี่ยนรูป)
     * @param removeImage ตั้งค่าเป็น true หากต้องการลบรูปภาพที่มีอยู่ออก โดยไม่ต้องอัปโหลดใหม่
     * @return {@link ComponentResponse} ที่มีข้อมูลที่อัปเดตแล้ว
     * @throws ResponseStatusException หากไม่พบชิ้นส่วนตาม ID ที่ระบุ
     */
    ComponentResponse updateComponent(String componentId, ComponentRequest request, MultipartFile imageFile, boolean removeImage);

    /**
     * ปรับจำนวนสต็อกสินค้าในคลัง (เพิ่มหรือลด).
     * @param componentId ID ของชิ้นส่วนที่ต้องการปรับสต็อก
     * @param request DTO ที่มีจำนวนที่ต้องการเปลี่ยนแปลง (ใช้ค่าบวกสำหรับเพิ่ม, ค่าลบสำหรับลด)
     * @return {@link ComponentResponse} ที่มีจำนวนสต็อกล่าสุด
     * @throws ResponseStatusException หากจำนวนสต็อกคงเหลือไม่เพียงพอ (ในกรณีที่พยายามลดสต็อก)
     */
    ComponentResponse adjustStock(String componentId, StockAdjustmentRequest request);

    /**
     * ลบชิ้นส่วนออกจากระบบอย่างถาวร.
     * <p>
     * การดำเนินการนี้จะลบทั้งข้อมูลชิ้นส่วน, ข้อมูลในคลัง (Inventory), และไฟล์รูปภาพที่เกี่ยวข้องใน S3 ด้วย
     * </p>
     * @param componentId ID ของชิ้นส่วนที่ต้องการลบ
     */
    void deleteComponent(String componentId);

    /**
     * ดึงข้อมูลรายละเอียดของชิ้นส่วนเพียงชิ้นเดียวตาม ID ที่ระบุ.
     * @param componentId ID ของชิ้นส่วนที่ต้องการ
     * @return {@link ComponentResponse} ที่มีข้อมูลทั้งหมดของชิ้นส่วนนั้น
     * @throws ResponseStatusException หากไม่พบชิ้นส่วน
     */
    ComponentResponse getComponentDetailsById(String componentId);

    /**
     * ดึงรายการชิ้นส่วนทั้งหมดที่มีในระบบ.
     * @return {@code List<ComponentResponse>} รายการชิ้นส่วนทั้งหมด
     */
    List<ComponentResponse> getAllComponents();

}