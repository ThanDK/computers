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

/**
 * Controller สำหรับจัดการข้อมูลอ้างอิง (Lookup Data) ของระบบ เช่น Sockets, RAM Types, Brands.
 * <p>
 * ทุก Endpoint ในคลาสนี้ต้องมีการยืนยันตัวตนและมีสิทธิ์เป็น 'ADMIN' เท่านั้น
 */
@RestController
@RequestMapping("/api/admin/lookups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LookupController {

    private final LookupService lookupService;

    /**
     * ดึงข้อมูล Lookup ทั้งหมดที่จำเป็นสำหรับใช้ในหน้าฟอร์มสร้าง/แก้ไขชิ้นส่วนคอมพิวเตอร์
     * <p>
     * เพื่อลดจำนวนการเรียก API จาก Frontend โดยจะรวมข้อมูลทั้งหมดไว้ใน Response เดียว
     * @return Map ที่มี key เป็นชื่อของ lookup (เช่น 'sockets', 'ramTypes') และ value เป็น List ของข้อมูลนั้นๆ
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLookupsForFormComponent() {
        return ResponseEntity.ok(lookupService.getAllLookups());
    }

    // --- Sockets Management ---
    /**
     * ดึงรายการ Sockets ทั้งหมด
     * @return List ของ Sockets
     */
    @GetMapping("/sockets")
    public ResponseEntity<List<Socket>> getAllSockets() {
        return ResponseEntity.ok(lookupService.getAllSockets());
    }

    /**
     * สร้าง Socket ใหม่
     * @param request ข้อมูล Socket ที่ต้องการสร้าง
     * @return Socket ที่สร้างสำเร็จ พร้อม HttpStatus 201 CREATED
     */
    @PostMapping("/sockets")
    public ResponseEntity<Socket> createSocket(@Valid @RequestBody SocketRequest request) {
        Socket createdSocket = lookupService.createSocket(request);
        return new ResponseEntity<>(createdSocket, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Socket ที่มีอยู่
     * @param id ID ของ Socket ที่ต้องการอัปเดต
     * @param request ข้อมูลใหม่สำหรับ Socket
     * @return Socket ที่อัปเดตแล้ว
     */
    @PutMapping("/sockets/{id}")
    public ResponseEntity<Socket> updateSocket(@PathVariable String id, @Valid @RequestBody SocketRequest request) {
        Socket updatedSocket = lookupService.updateSocket(id, request);
        return ResponseEntity.ok(updatedSocket);
    }

    /**
     * ลบ Socket ออกจากระบบ
     * @param id ID ของ Socket ที่ต้องการลบ
     * @return HttpStatus 204 NO_CONTENT หากลบสำเร็จ
     */
    @DeleteMapping("/sockets/{id}")
    public ResponseEntity<Void> deleteSocket(@PathVariable String id) {
        lookupService.deleteSocket(id);
        return ResponseEntity.noContent().build();
    }

    // --- RAM Types Management ---
    /**
     * ดึงรายการ Ram Types ทั้งหมด
     * @return List ของ Ram Types
     */
    @GetMapping("/ram-types")
    public ResponseEntity<List<RamType>> getAllRamTypes() {
        return ResponseEntity.ok(lookupService.getAllRamTypes());
    }

    /**
     * สร้าง Ram Type ใหม่
     * @param request ข้อมูล Ram Type ที่จะสร้าง
     * @return Ram Type ที่สร้างสำเร็จ พร้อม HttpStatus 201 CREATED
     */
    @PostMapping("/ram-types")
    public ResponseEntity<RamType> createRamType(@Valid @RequestBody RamTypeRequest request) {
        RamType createdRamType = lookupService.createRamType(request);
        return new ResponseEntity<>(createdRamType, HttpStatus.CREATED);
    }

    /**
     * อัปเดต Ram Type
     * @param id ID ของ Ram Type ที่จะอัปเดต
     * @param request ข้อมูลใหม่
     * @return Ram Type ที่อัปเดตแล้ว
     */
    @PutMapping("/ram-types/{id}")
    public ResponseEntity<RamType> updateRamType(@PathVariable String id, @Valid @RequestBody RamTypeRequest request) {
        RamType updatedRamType = lookupService.updateRamType(id, request);
        return ResponseEntity.ok(updatedRamType);
    }

    /**
     * ลบ Ram Type
     * @param id ID ของ Ram Type ที่จะลบ
     * @return HttpStatus 204 NO_CONTENT หากลบสำเร็จ
     */
    @DeleteMapping("/ram-types/{id}")
    public ResponseEntity<Void> deleteRamType(@PathVariable String id) {
        lookupService.deleteRamType(id);
        return ResponseEntity.noContent().build();
    }

    // --- Form Factors Management ---
    /**
     * ดึงรายการ Form Factors ทั้งหมด
     * @return List ของ Form Factors
     */
    @GetMapping("/form-factors")
    public ResponseEntity<List<FormFactor>> getAllFormFactors() {
        return ResponseEntity.ok(lookupService.getAllFormFactors());
    }

    /**
     * สร้าง Form Factor ใหม่
     * @param request ข้อมูล Form Factor ที่จะสร้าง
     * @return Form Factor ที่สร้างสำเร็จ พร้อม HttpStatus 201 CREATED
     */
    @PostMapping("/form-factors")
    public ResponseEntity<FormFactor> createFormFactor(@Valid @RequestBody FormFactorRequest request) {
        FormFactor createdFormFactor = lookupService.createFormFactor(request);
        return new ResponseEntity<>(createdFormFactor, HttpStatus.CREATED);
    }

    /**
     * อัปเดต Form Factor
     * @param id ID ของ Form Factor ที่จะอัปเดต
     * @param request ข้อมูลใหม่
     * @return Form Factor ที่อัปเดตแล้ว
     */
    @PutMapping("/form-factors/{id}")
    public ResponseEntity<FormFactor> updateFormFactor(@PathVariable String id, @Valid @RequestBody FormFactorRequest request) {
        FormFactor updatedFormFactor = lookupService.updateFormFactor(id, request);
        return ResponseEntity.ok(updatedFormFactor);
    }

    /**
     * ลบ Form Factor
     * @param id ID ของ Form Factor ที่จะลบ
     * @return HttpStatus 204 NO_CONTENT หากลบสำเร็จ
     */
    @DeleteMapping("/form-factors/{id}")
    public ResponseEntity<Void> deleteFormFactor(@PathVariable String id) {
        lookupService.deleteFormFactor(id);
        return ResponseEntity.noContent().build();
    }

    // --- Storage Interfaces Management ---
    /**
     * ดึงรายการ Storage Interfaces ทั้งหมด
     * @return List ของ Storage Interfaces
     */
    @GetMapping("/storage-interfaces")
    public ResponseEntity<List<StorageInterface>> getAllStorageInterfaces() {
        return ResponseEntity.ok(lookupService.getAllStorageInterfaces());
    }

    /**
     * สร้าง Storage Interface ใหม่
     * @param request ข้อมูล Storage Interface ที่จะสร้าง
     * @return Storage Interface ที่สร้างสำเร็จ พร้อม HttpStatus 201 CREATED
     */
    @PostMapping("/storage-interfaces")
    public ResponseEntity<StorageInterface> createStorageInterface(@Valid @RequestBody StorageInterfaceRequest request) {
        StorageInterface createdInterface = lookupService.createStorageInterface(request);
        return new ResponseEntity<>(createdInterface, HttpStatus.CREATED);
    }

    /**
     * อัปเดต Storage Interface
     * @param id ID ของ Storage Interface ที่จะอัปเดต
     * @param request ข้อมูลใหม่
     * @return Storage Interface ที่อัปเดตแล้ว
     */
    @PutMapping("/storage-interfaces/{id}")
    public ResponseEntity<StorageInterface> updateStorageInterface(@PathVariable String id, @Valid @RequestBody StorageInterfaceRequest request) {
        StorageInterface updatedInterface = lookupService.updateStorageInterface(id, request);
        return ResponseEntity.ok(updatedInterface);
    }

    /**
     * ลบ Storage Interface
     * @param id ID ของ Storage Interface ที่จะลบ
     * @return HttpStatus 204 NO_CONTENT หากลบสำเร็จ
     */
    @DeleteMapping("/storage-interfaces/{id}")
    public ResponseEntity<Void> deleteStorageInterface(@PathVariable String id) {
        lookupService.deleteStorageInterface(id);
        return ResponseEntity.noContent().build();
    }

    // --- Shipping Providers Management ---
    /**
     * ดึงรายการผู้ให้บริการจัดส่ง (Shipping Providers) ทั้งหมด
     * @return List ของ Shipping Providers
     */
    @GetMapping("/shipping-providers")
    public ResponseEntity<List<ShippingProvider>> getAllShippingProviders() {
        return ResponseEntity.ok(lookupService.getAllShippingProviders());
    }

    /**
     * สร้าง Shipping Provider ใหม่ พร้อมอัปโหลดโลโก้ (ถ้ามี)
     * <p>
     * รับข้อมูลแบบ multipart/form-data
     * @param request ข้อมูลของ Provider (JSON ใน part ที่ชื่อ "provider")
     * @param image ไฟล์รูปภาพโลโก้ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return Provider ที่สร้างสำเร็จ พร้อม HttpStatus 201 CREATED
     */
    @PostMapping(value = "/shipping-providers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShippingProvider> createShippingProvider(
            @RequestPart("provider") @Valid ShippingProviderRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ShippingProvider createdProvider = lookupService.createShippingProvider(request, image);
        return new ResponseEntity<>(createdProvider, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Shipping Provider และโลโก้ (ถ้ามีการส่งไฟล์ใหม่มา)
     * <p>
     * รับข้อมูลแบบ multipart/form-data
     * @param id ID ของ Provider ที่จะอัปเดต
     * @param request ข้อมูลใหม่ (JSON ใน part ที่ชื่อ "provider")
     * @param image รูปภาพโลโก้ใหม่ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return Provider ที่อัปเดตแล้ว
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
     * ลบ Shipping Provider ออกจากระบบ
     * @param id ID ของ Provider ที่ต้องการลบ
     * @return HttpStatus 204 NO_CONTENT หากลบสำเร็จ
     */
    @DeleteMapping("/shipping-providers/{id}")
    public ResponseEntity<Void> deleteShippingProvider(@PathVariable String id) {
        lookupService.deleteShippingProvider(id);
        return ResponseEntity.noContent().build();
    }

    // --- Brands Management ---
    /**
     * ดึงรายการ Brands ทั้งหมด
     * @return List ของ Brands
     */
    @GetMapping("/brands")
    public ResponseEntity<List<Brand>> getAllBrands() {
        return ResponseEntity.ok(lookupService.getAllBrands());
    }

    /**
     * สร้าง Brand ใหม่ พร้อมอัปโหลดโลโก้ (ถ้ามี)
     * <p>
     * รับข้อมูลแบบ multipart/form-data
     * @param request ข้อมูล Brand (JSON ใน part ที่ชื่อ "brand")
     * @param image ไฟล์รูปภาพโลโก้ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return Brand ที่สร้างสำเร็จ พร้อม HttpStatus 201 CREATED
     */
    @PostMapping(value = "/brands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Brand> createBrand(
            @RequestPart("brand") @Valid BrandRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Brand createdBrand = lookupService.createBrand(request, image);
        return new ResponseEntity<>(createdBrand, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูล Brand และโลโก้ (ถ้ามีการส่งไฟล์ใหม่มา)
     * <p>
     * รับข้อมูลแบบ multipart/form-data
     * @param id ID ของ Brand ที่จะอัปเดต
     * @param request ข้อมูลใหม่ (JSON ใน part ที่ชื่อ "brand")
     * @param image รูปภาพโลโก้ใหม่ (เป็นทางเลือก, ใน part ที่ชื่อ "image")
     * @return Brand ที่อัปเดตแล้ว
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
     * ลบ Brand ออกจากระบบ
     * @param id ID ของ Brand ที่จะลบ
     * @return HttpStatus 204 NO_CONTENT หากลบสำเร็จ
     */
    @DeleteMapping("/brands/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable String id) {
        lookupService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}