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
 * Controller สำหรับจัดการข้อมูลอ้างอิง (Lookup Data) ทั้งหมดในระบบ
 * เช่น Sockets, RAM Types, Brands ซึ่งต้องใช้สิทธิ์ Admin เท่านั้น
 */
@RestController
@RequestMapping("/api/admin/lookups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LookupController {

    private final LookupService lookupService;

    /**
     * ดึงข้อมูล Lookup ทั้งหมดสำหรับใช้ในฟอร์มสร้าง/แก้ไขชิ้นส่วน
     * @return Map ที่มี key เป็นชื่อของ lookup และ value เป็น List ของข้อมูลนั้นๆ
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
     * @param request ข้อมูล Socket ที่จะสร้าง
     * @return Socket ที่สร้างสำเร็จ (HttpStatus 201)
     */
    @PostMapping("/sockets")
    public ResponseEntity<Socket> createSocket(@Valid @RequestBody SocketRequest request) {
        Socket createdSocket = lookupService.createSocket(request);
        return new ResponseEntity<>(createdSocket, HttpStatus.CREATED);
    }

    /**
     * อัปเดต Socket
     * @param id ID ของ Socket ที่จะอัปเดต
     * @param request ข้อมูลใหม่
     * @return Socket ที่อัปเดตแล้ว
     */
    @PutMapping("/sockets/{id}")
    public ResponseEntity<Socket> updateSocket(@PathVariable String id, @Valid @RequestBody SocketRequest request) {
        Socket updatedSocket = lookupService.updateSocket(id, request);
        return ResponseEntity.ok(updatedSocket);
    }

    /**
     * ลบ Socket (คืนค่า 204 No Content)
     * @param id ID ของ Socket ที่จะลบ
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
     * @return Ram Type ที่สร้างสำเร็จ (HttpStatus 201)
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
     * ลบ Ram Type (คืนค่า 204 No Content)
     * @param id ID ของ Ram Type ที่จะลบ
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
     * @return Form Factor ที่สร้างสำเร็จ (HttpStatus 201)
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
     * ลบ Form Factor (คืนค่า 204 No Content)
     * @param id ID ของ Form Factor ที่จะลบ
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
     * @return Storage Interface ที่สร้างสำเร็จ (HttpStatus 201)
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
     * ลบ Storage Interface (คืนค่า 204 No Content)
     * @param id ID ของ Storage Interface ที่จะลบ
     */
    @DeleteMapping("/storage-interfaces/{id}")
    public ResponseEntity<Void> deleteStorageInterface(@PathVariable String id) {
        lookupService.deleteStorageInterface(id);
        return ResponseEntity.noContent().build();
    }

    // --- Shipping Providers Management ---
    /**
     * ดึงรายการ Shipping Providers ทั้งหมด
     * @return List ของ Shipping Providers
     */
    @GetMapping("/shipping-providers")
    public ResponseEntity<List<ShippingProvider>> getAllShippingProviders() {
        return ResponseEntity.ok(lookupService.getAllShippingProviders());
    }

    /**
     * สร้าง Shipping Provider ใหม่ (รับข้อมูลแบบ multipart/form-data)
     * @param request ข้อมูล Provider (part: "provider")
     * @param image รูปภาพโลโก้ (optional, part: "image")
     * @return Provider ที่สร้างสำเร็จ (HttpStatus 201)
     */
    @PostMapping(value = "/shipping-providers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShippingProvider> createShippingProvider(
            @RequestPart("provider") @Valid ShippingProviderRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ShippingProvider createdProvider = lookupService.createShippingProvider(request, image);
        return new ResponseEntity<>(createdProvider, HttpStatus.CREATED);
    }

    /**
     * อัปเดต Shipping Provider (รับข้อมูลแบบ multipart/form-data)
     * @param id ID ของ Provider ที่จะอัปเดต
     * @param request ข้อมูลใหม่ (part: "provider")
     * @param image รูปภาพโลโก้ใหม่ (optional, part: "image")
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
     * ลบ Shipping Provider (คืนค่า 204 No Content)
     * @param id ID ของ Provider ที่จะลบ
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
     * สร้าง Brand ใหม่ (รับข้อมูลแบบ multipart/form-data)
     * @param request ข้อมูล Brand (part: "brand")
     * @param image รูปภาพโลโก้ (optional, part: "image")
     * @return Brand ที่สร้างสำเร็จ (HttpStatus 201)
     */
    @PostMapping(value = "/brands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Brand> createBrand(
            @RequestPart("brand") @Valid BrandRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Brand createdBrand = lookupService.createBrand(request, image);
        return new ResponseEntity<>(createdBrand, HttpStatus.CREATED);
    }

    /**
     * อัปเดต Brand (รับข้อมูลแบบ multipart/form-data)
     * @param id ID ของ Brand ที่จะอัปเดต
     * @param request ข้อมูลใหม่ (part: "brand")
     * @param image รูปภาพโลโก้ใหม่ (optional, part: "image")
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
     * ลบ Brand (คืนค่า 204 No Content)
     * @param id ID ของ Brand ที่จะลบ
     */
    @DeleteMapping("/brands/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable String id) {
        lookupService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}