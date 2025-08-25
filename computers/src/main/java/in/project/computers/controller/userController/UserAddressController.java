package in.project.computers.controller.userController;

import in.project.computers.DTO.address.AddressDTO;
import in.project.computers.service.addressService.AddressService;
import in.project.computers.service.userAuthenticationService.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class UserAddressController {

    private final AddressService addressService;
    private final UserService userService;

    /**
     * ดึงที่อยู่สำหรับจัดส่งทั้งหมดของผู้ใช้
     * <p>
     * Endpoint นี้สำหรับให้ผู้ใช้ที่ล็อกอินแล้ว ดึงข้อมูลที่อยู่สำหรับจัดส่งทั้งหมดที่เคยบันทึกไว้ในบัญชีของตนเอง
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link AddressDTO} และสถานะ 200 OK
     */
    @GetMapping
    public ResponseEntity<List<AddressDTO>> getUserAddresses() {
        String userId = userService.findByUserId();
        log.info("Authenticated user ({}) is fetching their addresses.", userId);
        List<AddressDTO> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    /**
     * เพิ่มที่อยู่สำหรับจัดส่งใหม่
     * <p>
     * Endpoint นี้อนุญาตให้ผู้ใช้ที่ล็อกอินแล้ว เพิ่มที่อยู่สำหรับจัดส่งใหม่เข้าไปในบัญชีของตนเอง
     * </p>
     * @param request อ็อบเจกต์ {@link AddressDTO} ที่มีข้อมูลที่อยู่ใหม่ที่ต้องการเพิ่ม
     * @return ResponseEntity ที่มีข้อมูล {@link AddressDTO} ของที่อยู่ที่สร้างใหม่และสถานะ 201 Created
     */
    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@Valid @RequestBody AddressDTO request) {
        String userId = userService.findByUserId();
        log.info("User {} is adding a new address.", userId);
        AddressDTO newAddress = addressService.addAddress(userId, request);
        return new ResponseEntity<>(newAddress, HttpStatus.CREATED);
    }

    /**
     * อัปเดตข้อมูลที่อยู่ที่มีอยู่แล้ว
     * <p>
     * Endpoint นี้ใช้สำหรับแก้ไขข้อมูลที่อยู่สำหรับจัดส่งที่ผู้ใช้เคยบันทึกไว้แล้ว ระบบจะตรวจสอบความเป็นเจ้าของก่อนทำการอัปเดต
     * </p>
     * @param addressId ID ของที่อยู่ที่จะอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link AddressDTO} ที่มีข้อมูลที่อยู่ใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link AddressDTO} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable String addressId, @Valid @RequestBody AddressDTO request) {
        String userId = userService.findByUserId();
        log.info("User {} is updating address ID: {}", userId, addressId);
        AddressDTO updatedAddress = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * ลบที่อยู่สำหรับจัดส่ง
     * <p>
     * Endpoint นี้สำหรับให้ผู้ใช้ลบที่อยู่สำหรับจัดส่งที่ไม่ต้องการแล้วออกจากบัญชี ระบบจะตรวจสอบความเป็นเจ้าของก่อนทำการลบ
     * เมื่อดำเนินการสำเร็จจะคืนสถานะ 204 No Content
     * </p>
     * @param addressId ID ของที่อยู่ที่จะลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 204 No Content
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable String addressId) {
        String userId = userService.findByUserId();
        log.info("User {} is deleting address ID: {}", userId, addressId);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ตั้งค่าที่อยู่ให้เป็นที่อยู่หลัก
     * <p>
     * Endpoint นี้ใช้สำหรับกำหนดให้ที่อยู่รายการใดรายการหนึ่งเป็นที่อยู่เริ่มต้น (Default)
     * สำหรับการจัดส่งสินค้าในครั้งต่อไป
     * </p>
     * @param addressId ID ของที่อยู่ที่จะตั้งเป็นหลัก (จาก Path Variable)
     * @return ResponseEntity ที่มีสถานะ 200 OK เพื่อยืนยันการทำงานสำเร็จ
     */
    @PostMapping("/set-default/{addressId}")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable String addressId) {
        String userId = userService.findByUserId();
        log.info("User {} is setting address ID {} as default.", userId, addressId);
        addressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok().build();
    }
}