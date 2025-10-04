package in.project.computers.service.addressService;

import in.project.computers.DTO.address.AddressRequest;
import in.project.computers.DTO.address.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getUserAddresses(String userId);

    AddressResponse getAddressById(String userId, String addressId);

    AddressResponse addAddress(String userId, AddressRequest addressRequest);

    AddressResponse updateAddress(String userId, String addressId, AddressRequest addressRequest);

    void deleteAddress(String userId, String addressId);

    void setDefaultAddress(String userId, String addressId);
}