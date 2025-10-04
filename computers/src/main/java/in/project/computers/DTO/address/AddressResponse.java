package in.project.computers.DTO.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private String id;
    private String contactName;
    private String phoneNumber;
    private String line1;
    private String line2;
    private String subdistrict;
    private String district;
    private String province;
    private String zipCode;
    private String country;

    @JsonProperty("isDefault")
    private boolean isDefault;
}