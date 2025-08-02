package in.project.computers.service.componentService;

import in.project.computers.DTO.lookup.*;
import in.project.computers.entity.lookup.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface LookupService {

    /**
     * ดึงข้อมูล Lookup ทั้งหมดที่จำเป็นสำหรับ UI ในการเรียกเพียงครั้งเดียว
     * @return Map ที่มี Key เป็นชื่อประเภทของ Lookup และ Value เป็น List ของข้อมูลนั้นๆ
     */
    Map<String, Object> getAllLookups();

    /**
     * กลุ่มเมธอดสำหรับดึงข้อมูล Lookup แต่ละประเภทโดยเฉพาะ
     */
    List<Socket> getAllSockets();
    List<RamType> getAllRamTypes();
    List<FormFactor> getAllFormFactors();
    List<StorageInterface> getAllStorageInterfaces();
    List<ShippingProvider> getAllShippingProviders();
    List<Brand> getAllBrands();

    /**
     * กลุ่มเมธอดสำหรับจัดการข้อมูลพื้นฐาน (Socket, RAM Type, Form Factor, Storage Interface)
     * มีรูปแบบการทำงานที่คล้ายกันคือ:
     * <ul>
     *     <li><b>Create:</b> สร้างข้อมูลใหม่ โดยมีการตรวจสอบชื่อซ้ำ</li>
     *     <li><b>Update:</b> แก้ไขข้อมูลที่มีอยู่ โดยมีการป้องกันการเปลี่ยนชื่อหากข้อมูลนั้นถูกใช้งานอยู่</li>
     *     <li><b>Delete:</b> ลบข้อมูล โดยมีการป้องกันการลบหากข้อมูลนั้นถูกใช้งานอยู่</li>
     * </ul>
     */
    Socket createSocket(SocketRequest request);
    Socket updateSocket(String id, SocketRequest request);
    void deleteSocket(String id);

    RamType createRamType(RamTypeRequest request);
    RamType updateRamType(String id, RamTypeRequest request);
    void deleteRamType(String id);

    FormFactor createFormFactor(FormFactorRequest request);
    FormFactor updateFormFactor(String id, FormFactorRequest request);
    void deleteFormFactor(String id);

    StorageInterface createStorageInterface(StorageInterfaceRequest request);
    StorageInterface updateStorageInterface(String id, StorageInterfaceRequest request);
    void deleteStorageInterface(String id);

    /**
     * กลุ่มเมธอดสำหรับจัดการข้อมูลที่มีรูปภาพ (Shipping Provider, Brand)
     * มีรูปแบบการทำงานเพิ่มเติมคือ:
     * <ul>
     *     <li><b>Create:</b> สามารถอัปโหลดรูปภาพไปพร้อมกับการสร้างข้อมูลได้</li>
     *     <li><b>Update:</b> สามารถอัปเดตข้อมูลพร้อมกับเปลี่ยนรูปภาพ (โดยจะลบรูปเก่าออก)</li>
     *     <li><b>Delete:</b> จะทำการลบรูปภาพที่เกี่ยวข้องออกจากระบบจัดเก็บไฟล์ (S3) ด้วย</li>
     * </ul>
     */
    ShippingProvider createShippingProvider(ShippingProviderRequest request, MultipartFile image);
    ShippingProvider updateShippingProvider(String id, ShippingProviderRequest request, MultipartFile image);
    void deleteShippingProvider(String id);

    Brand createBrand(BrandRequest request, MultipartFile image);
    Brand updateBrand(String id, BrandRequest request, MultipartFile image);
    void deleteBrand(String id);
}