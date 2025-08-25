package in.project.computers.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String id;
    private String contactName;
    private String phoneNumber;
    private String line1;
    private String line2;
    private String subdistrict;
    private String district;
    private String province;
    private String zipCode;

    @Builder.Default
    private String country = "Thailand";

    private boolean isDefault;
}