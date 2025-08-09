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

/**
 * Controller สำหรับจัดการที่อยู่สำหรับจัดส่งของผู้ใช้
 * <p>
 * ทุก Endpoint ในคลาสนี้ต้องการการยืนยันตัวตน (Authentication)
 */
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
     * @return List ของที่อยู่ทั้งหมด
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
     * @param request ข้อมูลที่อยู่ใหม่
     * @return ที่อยู่ที่สร้างสำเร็จ (HttpStatus 201)
     */
    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@Valid @RequestBody AddressDTO request) {
        String userId = userService.findByUserId();
        log.info("User {} is adding a new address.", userId);
        AddressDTO newAddress = addressService.addAddress(userId, request);
        return new ResponseEntity<>(newAddress, HttpStatus.CREATED);
    }

    /**
     * อัปเดตที่อยู่ที่มีอยู่แล้ว
     * @param addressId ID ของที่อยู่ที่จะอัปเดต
     * @param request ข้อมูลที่อยู่ใหม่
     * @return ที่อยู่ที่อัปเดตแล้ว
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable String addressId, @Valid @RequestBody AddressDTO request) {
        String userId = userService.findByUserId();
        log.info("User {} is updating address ID: {}", userId, addressId);
        AddressDTO updatedAddress = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * ลบที่อยู่ (คืนค่า 204 No Content)
     * @param addressId ID ของที่อยู่ที่จะลบ
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
     * @param addressId ID ของที่อยู่ที่จะตั้งเป็นหลัก
     */
    @PostMapping("/set-default/{addressId}")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable String addressId) {
        String userId = userService.findByUserId();
        log.info("User {} is setting address ID {} as default.", userId, addressId);
        addressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok().build();
    }
}