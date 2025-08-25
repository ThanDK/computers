package in.project.computers.controller.adminController;

import in.project.computers.DTO.component.componentRequest.ComponentRequest;
import in.project.computers.DTO.component.componentRequest.StockAdjustmentRequest;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import in.project.computers.service.componentService.ComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Slf4j
public class AdminComponentController {

    private final ComponentService componentService;

    /**
     * ดึงรายการชิ้นส่วนคอมพิวเตอร์ทั้งหมด
     * <p>
     * Endpoint นี้เปิดให้เข้าถึงได้ทั่วไปเพื่อแสดงรายการสินค้าทั้งหมดในระบบ
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link ComponentResponse} และสถานะ 200 OK
     */
    @GetMapping
    public ResponseEntity<List<ComponentResponse>> getAllComponents() {
        log.info("Request to fetch all components");
        List<ComponentResponse> components = componentService.getAllComponents();
        return ResponseEntity.ok(components);
    }

    /**
     * ดึงข้อมูลชิ้นส่วนคอมพิวเตอร์ตาม ID ที่ระบุ
     * <p>
     * Endpoint นี้ต้องการการยืนยันตัวตน (Authenticated User) เพื่อเข้าถึงข้อมูล
     * </p>
     * @param id ID ของชิ้นส่วนที่ต้องการดึงข้อมูล (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link ComponentResponse} ของชิ้นส่วนและสถานะ 200 OK
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComponentResponse> getComponentById(@PathVariable String id) {
        log.info("Request to fetch component with ID: {}", id);
        ComponentResponse component = componentService.getComponentDetailsById(id);
        return ResponseEntity.ok(component);
    }

    /**
     * สร้างชิ้นส่วนคอมพิวเตอร์ใหม่
     * <p>
     * Endpoint นี้รับข้อมูลแบบ multipart/form-data เพื่อสร้างชิ้นส่วนใหม่พร้อมกับอัปโหลดรูปภาพ
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param request อ็อบเจกต์ {@link ComponentRequest} ที่มีข้อมูลของชิ้นส่วน (ส่งมาใน part ชื่อ "request")
     * @param imageFile ไฟล์รูปภาพของชิ้นส่วน (เป็นทางเลือก, ส่งมาใน part ชื่อ "image")
     * @return ResponseEntity ที่มีข้อมูล {@link ComponentResponse} ของชิ้นส่วนที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentResponse> createComponent(
            @Valid @RequestPart("request") ComponentRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        log.info("Admin action: Creating new component with MPN: {}", request.getMpn());
        ComponentResponse newComponent = componentService.createComponent(request, imageFile);
        return new ResponseEntity<>(newComponent, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูลชิ้นส่วนคอมพิวเตอร์
     * <p>
     * Endpoint นี้ใช้สำหรับแก้ไขข้อมูลของชิ้นส่วนที่มีอยู่แล้ว สามารถอัปเดตข้อมูล, เปลี่ยนรูปภาพ, หรือลบรูปภาพเดิมได้
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param id ID ของชิ้นส่วนที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link ComponentRequest} ที่มีข้อมูลใหม่ (ส่งมาใน part ชื่อ "request")
     * @param imageFile ไฟล์รูปภาพใหม่ที่ต้องการเปลี่ยน (เป็นทางเลือก, ส่งมาใน part ชื่อ "image")
     * @param removeImage ตั้งค่าเป็น true หากต้องการลบรูปภาพเดิม (จาก Query Parameter)
     * @return ResponseEntity ที่มีข้อมูล {@link ComponentResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentResponse> updateComponent(
            @PathVariable String id,
            @Valid @RequestPart("request") ComponentRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "removeImage", defaultValue = "false") boolean removeImage) {
        log.info("Admin action: Updating component with ID: {}. Remove image flag: {}", id, removeImage);
        ComponentResponse updatedComponent = componentService.updateComponent(id, request, imageFile, removeImage);
        return ResponseEntity.ok(updatedComponent);
    }

    /**
     * ปรับปรุงจำนวนสต็อกของชิ้นส่วน
     * <p>
     * Endpoint นี้ใช้สำหรับเพิ่มหรือลดจำนวนสต็อกของสินค้า รับข้อมูลเป็น JSON
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param id ID ของชิ้นส่วนที่ต้องการปรับสต็อก (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link StockAdjustmentRequest} ที่มีจำนวนที่ต้องการปรับ (ค่าบวกสำหรับเพิ่ม, ค่าลบสำหรับลด)
     * @return ResponseEntity ที่มีข้อมูล {@link ComponentResponse} พร้อมจำนวนสต็อกล่าสุดและสถานะ 200 OK
     */
    @PatchMapping("/stock/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentResponse> adjustStock(
            @PathVariable String id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        log.info("Admin action: Adjusting stock for component ID: {} by {}", id, request.getQuantity());
        ComponentResponse updatedComponent = componentService.adjustStock(id, request);
        return ResponseEntity.ok(updatedComponent);
    }

    /**
     * ลบชิ้นส่วนคอมพิวเตอร์
     * <p>
     * Endpoint นี้ใช้สำหรับลบชิ้นส่วนออกจากระบบอย่างถาวร เมื่อดำเนินการสำเร็จจะคืนสถานะ 204 No Content
     * การเข้าถึงถูกจำกัดไว้สำหรับผู้ใช้ที่มี Role 'ADMIN' เท่านั้น
     * </p>
     * @param id ID ของชิ้นส่วนที่ต้องการลบ (จาก Path Variable)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteComponent(@PathVariable String id) {
        log.info("Admin action: Deleting component with ID: {}", id);
        componentService.deleteComponent(id);
    }
}