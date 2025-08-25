package in.project.computers.service.addressService;

import in.project.computers.DTO.address.AddressDTO;
import in.project.computers.entity.user.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressConverter {

    public AddressDTO convertEntityToDto(Address entity) {
        if (entity == null) {
            return null;
        }
        return AddressDTO.builder()
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

    public Address convertDtoToEntity(AddressDTO dto) {
        if (dto == null) {
            return null;
        }
        return Address.builder()
                .id(dto.getId())
                .contactName(dto.getContactName())
                .phoneNumber(dto.getPhoneNumber())
                .line1(dto.getLine1())
                .line2(dto.getLine2())
                .subdistrict(dto.getSubdistrict())
                .district(dto.getDistrict())
                .province(dto.getProvince())
                .zipCode(dto.getZipCode())
                .country(dto.getCountry() != null && !dto.getCountry().isBlank() ? dto.getCountry() : "Thailand") // Default to Thailand if empty
                .isDefault(dto.isDefault())
                .build();
    }

    public void updateEntityFromDto(Address entity, AddressDTO dto) {
        entity.setContactName(dto.getContactName());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setLine1(dto.getLine1());
        entity.setLine2(dto.getLine2());
        entity.setSubdistrict(dto.getSubdistrict());
        entity.setDistrict(dto.getDistrict());
        entity.setProvince(dto.getProvince());
        entity.setZipCode(dto.getZipCode());
        if (dto.getCountry() != null && !dto.getCountry().isBlank()) {
            entity.setCountry(dto.getCountry());
        }
        entity.setDefault(dto.isDefault());
    }
}