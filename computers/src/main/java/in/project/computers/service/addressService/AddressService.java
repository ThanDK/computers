package in.project.computers.service.addressService;

import in.project.computers.DTO.address.AddressDTO;
import java.util.List;

/**
 * Interface สำหรับบริการจัดการที่อยู่ของผู้ใช้
 * <p>
 * กำหนดสัญญา (contract) สำหรับการดำเนินการต่างๆ ที่เกี่ยวกับที่อยู่ เช่น
 * การดึงข้อมูล, การเพิ่ม, การแก้ไข, การลบ, และการตั้งค่าที่อยู่หลัก
 */
public interface AddressService {

    /**
     * ดึงที่อยู่สำหรับจัดส่งทั้งหมดของผู้ใช้ที่ระบุ
     *
     * @param userId ID ของผู้ใช้ที่ต้องการดึงข้อมูลที่อยู่
     * @return รายการ (List) ของที่อยู่ทั้งหมดในรูปแบบ AddressDTO
     */
    List<AddressDTO> getUserAddresses(String userId);

    /**
     * ดึงข้อมูลที่อยู่เฉพาะเจาะจงตาม ID โดยตรวจสอบว่าเป็นของผู้ใช้ที่ระบุหรือไม่
     *
     * @param userId    ID ของผู้ใช้เจ้าของที่อยู่
     * @param addressId ID ของที่อยู่ที่ต้องการดึงข้อมูล
     * @return ข้อมูลที่อยู่ในรูปแบบ AddressDTO
     */
    AddressDTO getAddressById(String userId, String addressId);

    /**
     * เพิ่มที่อยู่สำหรับจัดส่งใหม่ให้กับผู้ใช้
     *
     * @param userId     ID ของผู้ใช้ที่จะเพิ่มที่อยู่ให้
     * @param addressDto ข้อมูลที่อยู่ใหม่ที่ต้องการเพิ่ม
     * @return ที่อยู่ที่สร้างขึ้นใหม่ในรูปแบบ AddressDTO (พร้อม ID ที่ถูกสร้างขึ้น)
     */
    AddressDTO addAddress(String userId, AddressDTO addressDto);

    /**
     * อัปเดตข้อมูลที่อยู่ที่มีอยู่แล้ว
     *
     * @param userId     ID ของผู้ใช้เจ้าของที่อยู่
     * @param addressId  ID ของที่อยู่ที่ต้องการอัปเดต
     * @param addressDto ข้อมูลที่อยู่ใหม่
     * @return ที่อยู่ที่อัปเดตแล้วในรูปแบบ AddressDTO
     */
    AddressDTO updateAddress(String userId, String addressId, AddressDTO addressDto);

    /**
     * ลบที่อยู่ของผู้ใช้
     *
     * @param userId    ID ของผู้ใช้เจ้าของที่อยู่
     * @param addressId ID ของที่อยู่ที่ต้องการลบ
     */
    void deleteAddress(String userId, String addressId);

    /**
     * ตั้งค่าที่อยู่ให้เป็นที่อยู่หลัก (Default) สำหรับผู้ใช้
     *
     * @param userId    ID ของผู้ใช้
     * @param addressId ID ของที่อยู่ที่จะตั้งเป็นที่อยู่หลัก
     */
    void setDefaultAddress(String userId, String addressId);
}