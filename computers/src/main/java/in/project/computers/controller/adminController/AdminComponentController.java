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

/**
 * Controller สำหรับจัดการข้อมูลชิ้นส่วนคอมพิวเตอร์ (Component)
 */
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Slf4j
public class AdminComponentController {

    private final ComponentService componentService;

    /**
     * ดึงข้อมูลชิ้นส่วนคอมพิวเตอร์ทั้งหมด (Public)
     * @return List ของชิ้นส่วนทั้งหมด
     */
    @GetMapping
    public ResponseEntity<List<ComponentResponse>> getAllComponents() {
        log.info("Request to fetch all components");
        List<ComponentResponse> components = componentService.getAllComponents();
        return ResponseEntity.ok(components);
    }

    /**
     * ดึงข้อมูลชิ้นส่วนคอมพิวเตอร์ตาม ID
     * @param id ID ของชิ้นส่วนที่ต้องการ
     * @return ข้อมูลชิ้นส่วนที่ค้นพบ
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComponentResponse> getComponentById(@PathVariable String id) {
        log.info("Request to fetch component with ID: {}", id);
        ComponentResponse component = componentService.getComponentDetailsById(id);
        return ResponseEntity.ok(component);
    }

    /**
     * สร้างชิ้นส่วนคอมพิวเตอร์ใหม่ (Admin - รับข้อมูลแบบ multipart/form-data)
     * @param request ข้อมูลชิ้นส่วน (part: "request")
     * @param imageFile ไฟล์รูปภาพ (optional, part: "image")
     * @return ข้อมูลชิ้นส่วนที่สร้างใหม่ (HttpStatus 201)
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
     * อัปเดตข้อมูลชิ้นส่วนคอมพิวเตอร์ (Admin - รับข้อมูลแบบ multipart/form-data)
     * @param id ID ของชิ้นส่วนที่จะอัปเดต
     * @param request ข้อมูลชิ้นส่วนใหม่ (part: "request")
     * @param imageFile ไฟล์รูปภาพใหม่ (optional, part: "image")
     * @param removeImage ตั้งเป็น true เพื่อลบรูปภาพเดิม
     * @return ข้อมูลชิ้นส่วนหลังอัปเดต
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
     * ปรับปรุงจำนวนสต็อกสินค้า (Admin - ใช้ PATCH สำหรับ partial update)
     * @param id ID ของชิ้นส่วน
     * @param request ข้อมูลจำนวนที่ต้องการปรับ (+/-)
     * @return ข้อมูลชิ้นส่วนหลังปรับสต็อก
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
     * ลบชิ้นส่วนคอมพิวเตอร์ (Admin - คืนค่า 204 No Content)
     * @param id ID ของชิ้นส่วนที่จะลบ
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteComponent(@PathVariable String id) {
        log.info("Admin action: Deleting component with ID: {}", id);
        componentService.deleteComponent(id);
    }
}