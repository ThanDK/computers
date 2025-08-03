// package in.project.computers.controller.userController;

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
 * <h3>User Address Controller</h3>
 * <p>
 * Controller for handling all user-facing actions related to managing shipping addresses.
 * All endpoints require an authenticated user.
 * </p>
 */
@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class UserAddressController {

    private final AddressService addressService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getUserAddresses() {
        String userId = userService.findByUserId();
        log.info("Authenticated user ({}) is fetching their addresses.", userId);
        List<AddressDTO> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@Valid @RequestBody AddressDTO request) {
        String userId = userService.findByUserId();
        log.info("User {} is adding a new address.", userId);
        AddressDTO newAddress = addressService.addAddress(userId, request);
        return new ResponseEntity<>(newAddress, HttpStatus.CREATED);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable String addressId, @Valid @RequestBody AddressDTO request) {
        String userId = userService.findByUserId();
        log.info("User {} is updating address ID: {}", userId, addressId);
        AddressDTO updatedAddress = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(updatedAddress);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable String addressId) {
        String userId = userService.findByUserId();
        log.info("User {} is deleting address ID: {}", userId, addressId);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/set-default/{addressId}")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable String addressId) {
        String userId = userService.findByUserId();
        log.info("User {} is setting address ID {} as default.", userId, addressId);
        addressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok().build();
    }
}