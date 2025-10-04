package in.project.computers.service.addressService;

import in.project.computers.DTO.address.AddressRequest;
import in.project.computers.DTO.address.AddressResponse;
import in.project.computers.entity.user.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressConverter {

    /**
     * Converts an Address database entity into an AddressResponse DTO for sending to the client.
     * @param entity The Address entity from the database.
     * @return An AddressResponse object.
     */
    public AddressResponse convertEntityToResponse(Address entity) {
        if (entity == null) {
            return null;
        }
        return AddressResponse.builder()
                .id(entity.getId())
                .contactName(entity.getContactName())
                .phoneNumber(entity.getPhoneNumber())
                .line1(entity.getLine1())
                .line2(entity.getLine2())
                .subdistrict(entity.getSubdistrict())
                .district(entity.getDistrict())
                .province(entity.getProvince())
                .zipCode(entity.getZipCode())
                .country(entity.getCountry())
                .isDefault(entity.isDefault())
                .build();
    }

    /**
     * Converts an AddressRequest DTO from the client into a new Address database entity.
     * @param request The AddressRequest object from the client.
     * @return A new Address entity.
     */
    public Address convertRequestToEntity(AddressRequest request) {
        if (request == null) {
            return null;
        }
        return Address.builder()
                .id(request.getId())
                .contactName(request.getContactName())
                .phoneNumber(request.getPhoneNumber())
                .line1(request.getLine1())
                .line2(request.getLine2())
                .subdistrict(request.getSubdistrict())
                .district(request.getDistrict())
                .province(request.getProvince())
                .zipCode(request.getZipCode())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry() : "Thailand") // Default to Thailand if empty
                .isDefault(request.isDefault())
                .build();
    }

    /**
     * Updates an existing Address entity with data from an AddressRequest DTO.
     * @param entity The existing Address entity to update.
     * @param request The AddressRequest object containing new data.
     */
    public void updateEntityFromRequest(Address entity, AddressRequest request) {
        entity.setContactName(request.getContactName());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setLine1(request.getLine1());
        entity.setLine2(request.getLine2());
        entity.setSubdistrict(request.getSubdistrict());
        entity.setDistrict(request.getDistrict());
        entity.setProvince(request.getProvince());
        entity.setZipCode(request.getZipCode());
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            entity.setCountry(request.getCountry());
        }
        entity.setDefault(request.isDefault());
    }

    /**
     * Converts an AddressResponse DTO back into an Address database entity.
     * @param response The AddressResponse object.
     * @return An Address entity.
     */
    public Address convertResponseToEntity(AddressResponse response) {
        if (response == null) {
            return null;
        }
        return Address.builder()
                .id(response.getId())
                .contactName(response.getContactName())
                .phoneNumber(response.getPhoneNumber())
                .line1(response.getLine1())
                .line2(response.getLine2())
                .subdistrict(response.getSubdistrict())
                .district(response.getDistrict())
                .province(response.getProvince())
                .zipCode(response.getZipCode())
                .country(response.getCountry())
                .isDefault(response.isDefault())
                .build();
    }
}