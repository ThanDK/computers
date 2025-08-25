package in.project.computers.controller.pageController;

import in.project.computers.DTO.lookup.*;
import in.project.computers.entity.lookup.*;
import in.project.computers.service.componentService.LookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/lookups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LookupController {

    private final LookupService lookupService;

    /**
     * ดึงข้อมูล Lookup ทั้งหมดที่จำเป็นสำหรับฟอร์ม
     * <p>
     * Endpoint นี้ออกแบบมาเพื่อลดจำนวนการเรียก API จาก Frontend โดยจะรวบรวมข้อมูล Lookup ทั้งหมด
     * ที่ใช้ในหน้าฟอร์มสร้าง/แก้ไขชิ้นส่วนคอมพิวเตอร์ไว้ใน Response เดียว
     * </p>
     * @return ResponseEntity ที่มี Map ซึ่ง key คือชื่อของ lookup (เช่น 'sockets', 'ramTypes') และ value คือ List ของข้อมูลนั้นๆ
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLookupsForFormComponent() {
        return ResponseEntity.ok(lookupService.getAllLookups());
    }

    // --- Sockets Management ---

    /**
     * ดึงรายการ Sockets ทั้งหมด
     * <p>
     * Endpoint สำหรับดึงข้อมูล Sockets ทั้งหมดในระบบ เพื่อใช้ในส่วนของ Frontend เช่น การสร้าง Dropdown
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link Socket} และสถานะ 200 OK
     */
    @GetMapping("/sockets")
    public ResponseEntity<List<Socket>> getAllSockets() {
        return ResponseEntity.ok(lookupService.getAllSockets());
    }

    /**
     * สร้าง Socket ใหม่
     * <p>
     * Endpoint สำหรับเพิ่มข้อมูล Socket ใหม่เข้าไปในระบบ
     * </p>
     * @param request อ็อบเจกต์ {@link SocketRequest} ที่มีข้อมูลสำหรับสร้าง
     * @return ResponseEntity ที่มีข้อมูล {@link Socket} ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping("/sockets")
    public ResponseEntity<Socket> createSocket(@Valid @RequestBody SocketRequest request) {
        Socket createdSocket = lookupService.createSocket(request);
        return new ResponseEntity<>(createdSocket, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Socket
     * <p>
     * Endpoint สำหรับแก้ไขข้อมูล Socket ที่มีอยู่แล้วตาม ID ที่ระบุ
     * </p>
     * @param id ID ของ Socket ที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link SocketRequest} ที่มีข้อมูลใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link Socket} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/sockets/{id}")
    public ResponseEntity<Socket> updateSocket(@PathVariable String id, @Valid @RequestBody SocketRequest request) {
        Socket updatedSocket = lookupService.updateSocket(id, request);
        return ResponseEntity.ok(updatedSocket);
    }

    /**
     * ลบ Socket
     * <p>
     * Endpoint สำหรับลบข้อมูล Socket ออกจากระบบอย่างถาวร
     * </p>
     * @param id ID ของ Socket ที่ต้องการลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/sockets/{id}")
    public ResponseEntity<Void> deleteSocket(@PathVariable String id) {
        lookupService.deleteSocket(id);
        return ResponseEntity.noContent().build();
    }

    // --- RAM Types Management ---

    /**
     * ดึงรายการ Ram Types ทั้งหมด
     * <p>
     * Endpoint สำหรับดึงข้อมูล Ram Types ทั้งหมดในระบบ เพื่อใช้ในส่วนของ Frontend
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link RamType} และสถานะ 200 OK
     */
    @GetMapping("/ram-types")
    public ResponseEntity<List<RamType>> getAllRamTypes() {
        return ResponseEntity.ok(lookupService.getAllRamTypes());
    }

    /**
     * สร้าง Ram Type ใหม่
     * <p>
     * Endpoint สำหรับเพิ่มข้อมูล Ram Type ใหม่เข้าไปในระบบ
     * </p>
     * @param request อ็อบเจกต์ {@link RamTypeRequest} ที่มีข้อมูลสำหรับสร้าง
     * @return ResponseEntity ที่มีข้อมูล {@link RamType} ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping("/ram-types")
    public ResponseEntity<RamType> createRamType(@Valid @RequestBody RamTypeRequest request) {
        RamType createdRamType = lookupService.createRamType(request);
        return new ResponseEntity<>(createdRamType, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Ram Type
     * <p>
     * Endpoint สำหรับแก้ไขข้อมูล Ram Type ที่มีอยู่แล้วตาม ID ที่ระบุ
     * </p>
     * @param id ID ของ Ram Type ที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link RamTypeRequest} ที่มีข้อมูลใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link RamType} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/ram-types/{id}")
    public ResponseEntity<RamType> updateRamType(@PathVariable String id, @Valid @RequestBody RamTypeRequest request) {
        RamType updatedRamType = lookupService.updateRamType(id, request);
        return ResponseEntity.ok(updatedRamType);
    }

    /**
     * ลบ Ram Type
     * <p>
     * Endpoint สำหรับลบข้อมูล Ram Type ออกจากระบบอย่างถาวร
     * </p>
     * @param id ID ของ Ram Type ที่ต้องการลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/ram-types/{id}")
    public ResponseEntity<Void> deleteRamType(@PathVariable String id) {
        lookupService.deleteRamType(id);
        return ResponseEntity.noContent().build();
    }

    // --- Form Factors Management ---

    /**
     * ดึงรายการ Form Factors ทั้งหมด
     * <p>
     * Endpoint สำหรับดึงข้อมูล Form Factors ทั้งหมดในระบบ เพื่อใช้ในส่วนของ Frontend
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link FormFactor} และสถานะ 200 OK
     */
    @GetMapping("/form-factors")
    public ResponseEntity<List<FormFactor>> getAllFormFactors() {
        return ResponseEntity.ok(lookupService.getAllFormFactors());
    }

    /**
     * สร้าง Form Factor ใหม่
     * <p>
     * Endpoint สำหรับเพิ่มข้อมูล Form Factor ใหม่เข้าไปในระบบ
     * </p>
     * @param request อ็อบเจกต์ {@link FormFactorRequest} ที่มีข้อมูลสำหรับสร้าง
     * @return ResponseEntity ที่มีข้อมูล {@link FormFactor} ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping("/form-factors")
    public ResponseEntity<FormFactor> createFormFactor(@Valid @RequestBody FormFactorRequest request) {
        FormFactor createdFormFactor = lookupService.createFormFactor(request);
        return new ResponseEntity<>(createdFormFactor, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Form Factor
     * <p>
     * Endpoint สำหรับแก้ไขข้อมูล Form Factor ที่มีอยู่แล้วตาม ID ที่ระบุ
     * </p>
     * @param id ID ของ Form Factor ที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link FormFactorRequest} ที่มีข้อมูลใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link FormFactor} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/form-factors/{id}")
    public ResponseEntity<FormFactor> updateFormFactor(@PathVariable String id, @Valid @RequestBody FormFactorRequest request) {
        FormFactor updatedFormFactor = lookupService.updateFormFactor(id, request);
        return ResponseEntity.ok(updatedFormFactor);
    }

    /**
     * ลบ Form Factor
     * <p>
     * Endpoint สำหรับลบข้อมูล Form Factor ออกจากระบบอย่างถาวร
     * </p>
     * @param id ID ของ Form Factor ที่ต้องการลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/form-factors/{id}")
    public ResponseEntity<Void> deleteFormFactor(@PathVariable String id) {
        lookupService.deleteFormFactor(id);
        return ResponseEntity.noContent().build();
    }

    // --- Storage Interfaces Management ---

    /**
     * ดึงรายการ Storage Interfaces ทั้งหมด
     * <p>
     * Endpoint สำหรับดึงข้อมูล Storage Interfaces ทั้งหมดในระบบ เพื่อใช้ในส่วนของ Frontend
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link StorageInterface} และสถานะ 200 OK
     */
    @GetMapping("/storage-interfaces")
    public ResponseEntity<List<StorageInterface>> getAllStorageInterfaces() {
        return ResponseEntity.ok(lookupService.getAllStorageInterfaces());
    }

    /**
     * สร้าง Storage Interface ใหม่
     * <p>
     * Endpoint สำหรับเพิ่มข้อมูล Storage Interface ใหม่เข้าไปในระบบ
     * </p>
     * @param request อ็อบเจกต์ {@link StorageInterfaceRequest} ที่มีข้อมูลสำหรับสร้าง
     * @return ResponseEntity ที่มีข้อมูล {@link StorageInterface} ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping("/storage-interfaces")
    public ResponseEntity<StorageInterface> createStorageInterface(@Valid @RequestBody StorageInterfaceRequest request) {
        StorageInterface createdInterface = lookupService.createStorageInterface(request);
        return new ResponseEntity<>(createdInterface, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Storage Interface
     * <p>
     * Endpoint สำหรับแก้ไขข้อมูล Storage Interface ที่มีอยู่แล้วตาม ID ที่ระบุ
     * </p>
     * @param id ID ของ Storage Interface ที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link StorageInterfaceRequest} ที่มีข้อมูลใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link StorageInterface} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/storage-interfaces/{id}")
    public ResponseEntity<StorageInterface> updateStorageInterface(@PathVariable String id, @Valid @RequestBody StorageInterfaceRequest request) {
        StorageInterface updatedInterface = lookupService.updateStorageInterface(id, request);
        return ResponseEntity.ok(updatedInterface);
    }

    /**
     * ลบ Storage Interface
     * <p>
     * Endpoint สำหรับลบข้อมูล Storage Interface ออกจากระบบอย่างถาวร
     * </p>
     * @param id ID ของ Storage Interface ที่ต้องการลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/storage-interfaces/{id}")
    public ResponseEntity<Void> deleteStorageInterface(@PathVariable String id) {
        lookupService.deleteStorageInterface(id);
        return ResponseEntity.noContent().build();
    }

    // --- Shipping Providers Management ---

    /**
     * ดึงรายการผู้ให้บริการจัดส่งทั้งหมด
     * <p>
     * Endpoint สำหรับดึงข้อมูลผู้ให้บริการจัดส่งทั้งหมดในระบบ เพื่อใช้ในส่วนของ Frontend
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link ShippingProvider} และสถานะ 200 OK
     */
    @GetMapping("/shipping-providers")
    public ResponseEntity<List<ShippingProvider>> getAllShippingProviders() {
        return ResponseEntity.ok(lookupService.getAllShippingProviders());
    }

    /**
     * สร้างผู้ให้บริการจัดส่งใหม่พร้อมอัปโหลดโลโก้
     * <p>
     * Endpoint นี้รับข้อมูลแบบ multipart/form-data เพื่อสร้างผู้ให้บริการใหม่พร้อมกับอัปโหลดรูปภาพโลโก้ (ถ้ามี)
     * </p>
     * @param request ข้อมูลของผู้ให้บริการ (JSON ใน part ที่ชื่อ "provider")
     * @param image ไฟล์รูปภาพโลโก้ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return ResponseEntity ที่มีข้อมูล {@link ShippingProvider} ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping(value = "/shipping-providers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShippingProvider> createShippingProvider(
            @RequestPart("provider") @Valid ShippingProviderRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ShippingProvider createdProvider = lookupService.createShippingProvider(request, image);
        return new ResponseEntity<>(createdProvider, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูลผู้ให้บริการจัดส่งและโลโก้
     * <p>
     * Endpoint นี้ใช้สำหรับแก้ไขข้อมูลผู้ให้บริการที่มีอยู่แล้ว และสามารถเปลี่ยนหรือเพิ่มรูปภาพโลโก้ได้
     * </p>
     * @param id ID ของผู้ให้บริการที่ต้องการอัปเดต (จาก Path Variable)
     * @param request ข้อมูลใหม่ (JSON ใน part ที่ชื่อ "provider")
     * @param image รูปภาพโลโก้ใหม่ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return ResponseEntity ที่มีข้อมูล {@link ShippingProvider} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping(value = "/shipping-providers/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShippingProvider> updateShippingProvider(
            @PathVariable String id,
            @RequestPart("provider") @Valid ShippingProviderRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ShippingProvider updatedProvider = lookupService.updateShippingProvider(id, request, image);
        return ResponseEntity.ok(updatedProvider);
    }

    /**
     * ลบผู้ให้บริการจัดส่ง
     * <p>
     * Endpoint สำหรับลบข้อมูลผู้ให้บริการจัดส่งออกจากระบบอย่างถาวร
     * </p>
     * @param id ID ของผู้ให้บริการที่ต้องการลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/shipping-providers/{id}")
    public ResponseEntity<Void> deleteShippingProvider(@PathVariable String id) {
        lookupService.deleteShippingProvider(id);
        return ResponseEntity.noContent().build();
    }

    // --- Brands Management ---

    /**
     * ดึงรายการ Brands ทั้งหมด
     * <p>
     * Endpoint สำหรับดึงข้อมูล Brands ทั้งหมดในระบบ เพื่อใช้ในส่วนของ Frontend
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link Brand} และสถานะ 200 OK
     */
    @GetMapping("/brands")
    public ResponseEntity<List<Brand>> getAllBrands() {
        return ResponseEntity.ok(lookupService.getAllBrands());
    }

    /**
     * สร้าง Brand ใหม่พร้อมอัปโหลดโลโก้
     * <p>
     * Endpoint นี้รับข้อมูลแบบ multipart/form-data เพื่อสร้าง Brand ใหม่พร้อมกับอัปโหลดรูปภาพโลโก้ (ถ้ามี)
     * </p>
     * @param request ข้อมูล Brand (JSON ใน part ที่ชื่อ "brand")
     * @param image ไฟล์รูปภาพโลโก้ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return ResponseEntity ที่มีข้อมูล {@link Brand} ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping(value = "/brands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Brand> createBrand(
            @RequestPart("brand") @Valid BrandRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Brand createdBrand = lookupService.createBrand(request, image);
        return new ResponseEntity<>(createdBrand, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Brand และโลโก้
     * <p>
     * Endpoint นี้ใช้สำหรับแก้ไขข้อมูล Brand ที่มีอยู่แล้ว และสามารถเปลี่ยนหรือเพิ่มรูปภาพโลโก้ได้
     * </p>
     * @param id ID ของ Brand ที่จะอัปเดต (จาก Path Variable)
     * @param request ข้อมูลใหม่ (JSON ใน part ที่ชื่อ "brand")
     * @param image รูปภาพโลโก้ใหม่ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return ResponseEntity ที่มีข้อมูล {@link Brand} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping(value = "/brands/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Brand> updateBrand(
            @PathVariable String id,
            @RequestPart("brand") @Valid BrandRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Brand updatedBrand = lookupService.updateBrand(id, request, image);
        return ResponseEntity.ok(updatedBrand);
    }

    /**
     * ลบ Brand
     * <p>
     * Endpoint สำหรับลบข้อมูล Brand ออกจากระบบอย่างถาวร
     * </p>
     * @param id ID ของ Brand ที่จะลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/brands/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable String id) {
        lookupService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}