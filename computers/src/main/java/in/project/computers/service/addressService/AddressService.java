package in.project.computers.service.addressService;

import in.project.computers.DTO.address.AddressDTO;
import java.util.List;

public interface AddressService {

    /**
     * ดึงที่อยู่ทั้งหมดของผู้ใช้
     * @param userId ID ของผู้ใช้
     * @return รายการที่อยู่ของผู้ใช้
     */
    List<AddressDTO> getUserAddresses(String userId);

    /**
     * ดึงข้อมูลที่อยู่ตาม ID
     * @param userId ID ของผู้ใช้ที่เป็นเจ้าของที่อยู่
     * @param addressId ID ของที่อยู่
     * @return ข้อมูลที่อยู่
     */
    AddressDTO getAddressById(String userId, String addressId);

    /**
     * เพิ่มที่อยู่ใหม่
     * @param userId ID ของผู้ใช้
     * @param addressDto ข้อมูลที่อยู่ใหม่ที่จะเพิ่ม
     * @return ที่อยู่ที่ถูกเพิ่มเข้าไปใหม่ พร้อม ID
     */
    AddressDTO addAddress(String userId, AddressDTO addressDto);

    /**
     * อัปเดตข้อมูลที่อยู่
     * @param userId ID ของผู้ใช้ที่เป็นเจ้าของที่อยู่
     * @param addressId ID ของที่อยู่ที่จะอัปเดต
     * @param addressDto ข้อมูลที่อยู่ใหม่
     * @return ที่อยู่ที่อัปเดตแล้ว
     */
    AddressDTO updateAddress(String userId, String addressId, AddressDTO addressDto);

    /**
     * ลบที่อยู่
     * @param userId ID ของผู้ใช้ที่เป็นเจ้าของที่อยู่
     * @param addressId ID ของที่อยู่ที่จะลบ
     */
    void deleteAddress(String userId, String addressId);

    /**
     * ตั้งค่าที่อยู่เริ่มต้น
     * @param userId ID ของผู้ใช้
     * @param addressId ID ของที่อยู่ที่จะตั้งเป็นค่าเริ่มต้น
     */
    void setDefaultAddress(String userId, String addressId);
}