// package in.project.computers.service.addressService;

package in.project.computers.service.addressService;

import in.project.computers.DTO.address.AddressDTO;
import java.util.List;

public interface AddressService {
    List<AddressDTO> getUserAddresses(String userId);
    AddressDTO getAddressById(String userId, String addressId);
    AddressDTO addAddress(String userId, AddressDTO addressDto);
    AddressDTO updateAddress(String userId, String addressId, AddressDTO addressDto);
    void deleteAddress(String userId, String addressId);
    void setDefaultAddress(String userId, String addressId);
}